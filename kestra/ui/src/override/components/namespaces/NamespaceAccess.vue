<template>
    <section class="container">
        <div class="mb-3">
            <el-button type="primary" @click="openAddBinding">{{ $t('namespaceForm.add_access') }}</el-button>
        </div>
        <el-table v-loading="loading" :data="bindings" stripe>
            <el-table-column :label="$t('iam.role')">
                <template #default="scope">
                    {{ roleNameById(scope.row.roleId) }}
                </template>
            </el-table-column>
            <el-table-column :label="$t('users.username')">
                <template #default="scope">
                    {{ scope.row.userId ?? scope.row.groupId ?? '—' }}
                </template>
            </el-table-column>
            <el-table-column :label="$t('actions')" width="120" fixed="right">
                <template #default="scope">
                    <el-button link type="danger" size="small" @click="confirmDelete(scope.row)">
                        {{ $t('delete') }}
                    </el-button>
                </template>
            </el-table-column>
        </el-table>
        <div v-if="!loading && bindings.length === 0" class="p-4 text-muted">
            {{ $t('namespaceForm.no_access_assignments') }}
        </div>
        <el-dialog
            v-model="dialogVisible"
            :title="$t('namespaceForm.add_access')"
            destroy-on-close
            append-to-body
            width="450px"
            @close="resetForm"
        >
            <el-form ref="formRef" :model="form" :rules="formRules" label-position="top">
                <el-form-item :label="$t('iam.role')" prop="roleId">
                    <el-select v-model="form.roleId" :placeholder="$t('iam.select_role')" style="width: 100%">
                        <el-option
                            v-for="r in rolesList"
                            :key="r.id"
                            :label="r.name"
                            :value="r.id"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item :label="$t('users.username')" prop="userId">
                    <el-select v-model="form.userId" :placeholder="$t('iam.select_user')" style="width: 100%">
                        <el-option
                            v-for="u in usersList"
                            :key="u.id"
                            :label="u.username"
                            :value="u.id"
                        />
                    </el-select>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">{{ $t('cancel') }}</el-button>
                <el-button type="primary" :loading="saving" @click="submitBinding">
                    {{ $t('add') }}
                </el-button>
            </template>
        </el-dialog>
    </section>
</template>

<script setup lang="ts">
    import {computed, onMounted, ref, watch} from "vue";
    import {useI18n} from "vue-i18n";
    import type {FormInstance, FormRules} from "element-plus";
    import {ElMessageBox} from "element-plus";
    import {useBindingsStore, type Binding} from "../../../stores/bindings";
    import {useRolesStore, type Role} from "../../../stores/roles";
    import {useUsersStore, type User} from "../../../stores/users";

    const props = defineProps<{ namespace: string }>();

    const {t} = useI18n();
    const bindingsStore = useBindingsStore();
    const rolesStore = useRolesStore();
    const usersStore = useUsersStore();

    const loading = ref(false);
    const bindings = ref<Binding[]>([]);
    const rolesList = ref<Role[]>([]);
    const usersList = ref<User[]>([]);
    const dialogVisible = ref(false);
    const saving = ref(false);
    const formRef = ref<FormInstance>();
    const form = ref({roleId: "", userId: ""});
    const formRules: FormRules = {
        roleId: [{required: true, message: t("iam.role_required"), trigger: "change"}],
        userId: [{required: true, message: t("iam.user_required"), trigger: "change"}]
    };

    function roleNameById(id: string) {
        return rolesList.value.find((r) => r.id === id)?.name ?? id;
    }

    async function loadBindings() {
        if (!props.namespace) return;
        loading.value = true;
        try {
            bindings.value = await bindingsStore.list({namespace: props.namespace});
        } catch {
            bindings.value = [];
        } finally {
            loading.value = false;
        }
    }

    async function loadRolesAndUsers() {
        try {
            rolesList.value = await rolesStore.list();
            usersList.value = await usersStore.list();
        } catch {
            rolesList.value = [];
            usersList.value = [];
        }
    }

    function openAddBinding() {
        loadRolesAndUsers();
        form.value = {roleId: "", userId: ""};
        dialogVisible.value = true;
    }

    function resetForm() {
        formRef.value?.resetFields();
    }

    async function submitBinding() {
        if (!formRef.value) return;
        await formRef.value.validate(async (valid) => {
            if (!valid) return;
            saving.value = true;
            try {
                await bindingsStore.create(
                    form.value.roleId,
                    form.value.userId || undefined,
                    undefined,
                    props.namespace
                );
                dialogVisible.value = false;
                await loadBindings();
            } finally {
                saving.value = false;
            }
        });
    }

    async function confirmDelete(row: Binding) {
        try {
            await ElMessageBox.confirm(t("delete confirm", {name: row.id}));
            await bindingsStore.remove(row.id);
            await loadBindings();
        } catch {
            // cancelled
        }
    }

    onMounted(() => loadBindings());
    watch(() => props.namespace, () => loadBindings());
</script>
