<template>
    <TopNavBar :title="routeInfo.title">
        <template #additional-right>
            <el-button type="primary" @click="openCreate">
                {{ $t('users.add') }}
            </el-button>
        </template>
    </TopNavBar>
    <section class="container">
        <DataTable striped :total="users?.length ?? 0">
            <template #table>
                <NoData v-if="!users?.length" />
                <el-table v-else :data="users">
                    <el-table-column prop="username" :label="$t('users.username')" />
                    <el-table-column prop="disabled" :label="$t('users.disabled')" width="100">
                        <template #default="scope">
                            <el-tag :type="scope.row.disabled ? 'danger' : 'success'" size="small">
                                {{ scope.row.disabled ? $t('users.disabled_yes') : $t('users.disabled_no') }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column :label="$t('actions')" width="260" fixed="right">
                        <template #default="scope">
                            <el-button link type="primary" size="small" @click="openEdit(scope.row)">
                                {{ $t('edit') }}
                            </el-button>
                            <el-button link type="primary" size="small" @click="openResetPassword(scope.row)">
                                {{ $t('users.reset_password') }}
                            </el-button>
                            <el-button link type="danger" size="small" @click="confirmDelete(scope.row)">
                                {{ $t('delete') }}
                            </el-button>
                        </template>
                    </el-table-column>
                </el-table>
            </template>
        </DataTable>
        <el-dialog
            v-model="dialogVisible"
            :title="editingId ? $t('users.edit_title') : $t('users.create_title')"
            destroy-on-close
            append-to-body
            width="400px"
            @close="resetForm"
        >
            <el-form ref="formRef" :model="form" :rules="formRules" label-position="top">
                <el-form-item :label="$t('users.username')" prop="username">
                    <el-input v-model="form.username" :disabled="!!editingId" />
                </el-form-item>
                <el-form-item v-if="!editingId" :label="$t('users.password')" prop="password">
                    <el-input v-model="form.password" type="password" show-password />
                </el-form-item>
                <el-form-item v-else :label="$t('users.new_password_optional')" prop="password">
                    <el-input v-model="form.password" type="password" show-password :placeholder="$t('users.leave_blank_unchanged')" />
                </el-form-item>
                <el-form-item prop="disabled">
                    <el-checkbox v-model="form.disabled">{{ $t('users.disabled') }}</el-checkbox>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">{{ $t('cancel') }}</el-button>
                <el-button type="primary" :loading="saving" @click="submitForm">
                    {{ editingId ? $t('save') : $t('users.create') }}
                </el-button>
            </template>
        </el-dialog>
        <el-dialog
            v-model="resetPasswordVisible"
            :title="$t('users.reset_password')"
            destroy-on-close
            append-to-body
            width="400px"
        >
            <el-form ref="resetPasswordFormRef" :model="resetPasswordForm" :rules="resetPasswordFormRules" label-position="top">
                <el-form-item :label="$t('users.new_password')" prop="newPassword">
                    <el-input v-model="resetPasswordForm.newPassword" type="password" show-password />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="resetPasswordVisible = false">{{ $t('cancel') }}</el-button>
                <el-button type="primary" :loading="resetting" @click="submitResetPassword">
                    {{ $t('save') }}
                </el-button>
            </template>
        </el-dialog>
    </section>
</template>

<script lang="ts" setup>
    import {computed, onMounted, ref} from "vue";
    import {useI18n} from "vue-i18n";
    import {ElMessageBox} from "element-plus";
    import type {FormInstance, FormRules} from "element-plus";
    import TopNavBar from "../layout/TopNavBar.vue";
    import useRouteContext from "../../composables/useRouteContext";
    import {useUsersStore, type User} from "../../stores/users";
    import DataTable from "../layout/DataTable.vue";
    import NoData from "../layout/NoData.vue";

    const {t} = useI18n();
    const usersStore = useUsersStore();

    const routeInfo = computed(() => ({title: t("users.title")}));
    useRouteContext(routeInfo);

    const users = ref<User[]>([]);
    const dialogVisible = ref(false);
    const editingId = ref<string | null>(null);
    const saving = ref(false);
    const formRef = ref<FormInstance>();
    const form = ref({username: "", password: "", disabled: false});
    const formRules: FormRules = {
        username: [{required: true, message: t("users.username_required"), trigger: "blur"}],
        password: [
            {
                validator: (_rule, value, callback) => {
                    if (!editingId.value && (!value || value.length === 0)) {
                        callback(new Error(t("users.password_required")));
                    } else {
                        callback();
                    }
                },
                trigger: "blur"
            }
        ]
    };

    const resetPasswordVisible = ref(false);
    const resetUserId = ref<string | null>(null);
    const resetting = ref(false);
    const resetPasswordFormRef = ref<FormInstance>();
    const resetPasswordForm = ref({newPassword: ""});
    const resetPasswordFormRules: FormRules = {
        newPassword: [{required: true, message: t("users.new_password_required"), trigger: "blur"}]
    };

    async function load() {
        try {
            users.value = await usersStore.list();
        } catch {
            users.value = [];
        }
    }

    function resetFormData() {
        form.value = {username: "", password: "", disabled: false};
        editingId.value = null;
    }

    function openCreate() {
        resetFormData();
        dialogVisible.value = true;
    }

    function openEdit(row: User) {
        editingId.value = row.id;
        form.value = {username: row.username, password: "", disabled: row.disabled};
        dialogVisible.value = true;
    }

    function resetForm() {
        formRef.value?.resetFields();
        resetFormData();
    }

    async function submitForm() {
        if (!formRef.value) return;
        await formRef.value.validate(async (valid) => {
            if (!valid) return;
            saving.value = true;
            try {
                if (editingId.value) {
                    await usersStore.update(editingId.value, form.value.username, form.value.disabled, form.value.password);
                } else {
                    await usersStore.create(form.value.username, form.value.password, form.value.disabled);
                }
                dialogVisible.value = false;
                await load();
            } finally {
                saving.value = false;
            }
        });
    }

    function openResetPassword(row: User) {
        resetUserId.value = row.id;
        resetPasswordForm.value = {newPassword: ""};
        resetPasswordVisible.value = true;
    }

    async function submitResetPassword() {
        if (!resetPasswordFormRef.value || !resetUserId.value) return;
        await resetPasswordFormRef.value.validate(async (valid) => {
            if (!valid) return;
            resetting.value = true;
            try {
                await usersStore.resetPassword(resetUserId.value!, resetPasswordForm.value.newPassword);
                resetPasswordVisible.value = false;
            } finally {
                resetting.value = false;
            }
        });
    }

    async function confirmDelete(row: User) {
        try {
            await ElMessageBox.confirm(t("delete confirm", {name: row.username}));
            await usersStore.remove(row.id);
            await load();
        } catch {
            // cancelled
        }
    }

    onMounted(() => {
        load();
    });
</script>
