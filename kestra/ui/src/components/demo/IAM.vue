<template>
    <TopNavBar :title="routeInfo.title" />
    <section class="container">
        <el-tabs v-model="activeTab" class="iam-tabs">
            <el-tab-pane :label="$t('iam.roles')" name="roles">
                <div class="mb-3">
                    <el-button type="primary" @click="openRoleCreate">{{ $t('iam.add_role') }}</el-button>
                </div>
                <DataTable striped :total="roles?.length ?? 0">
                    <template #table>
                        <NoData v-if="!roles?.length" />
                        <el-table v-else :data="roles">
                            <el-table-column prop="name" :label="$t('iam.role_name')" />
                            <el-table-column prop="description" :label="$t('description')" />
                            <el-table-column :label="$t('iam.permissions')" min-width="200">
                                <template #default="scope">
                                    <el-tag v-for="p in scope.row.permissions?.slice(0, 5)" :key="p" size="small" class="mr-1 mb-1">
                                        {{ p }}
                                    </el-tag>
                                    <span v-if="scope.row.permissions?.length > 5">+{{ scope.row.permissions.length - 5 }}</span>
                                </template>
                            </el-table-column>
                            <el-table-column :label="$t('actions')" width="150" fixed="right">
                                <template #default="scope">
                                    <el-button link type="primary" size="small" @click="openRoleEdit(scope.row)">
                                        {{ $t('edit') }}
                                    </el-button>
                                    <el-button
                                        link
                                        type="danger"
                                        size="small"
                                        :disabled="scope.row.id === 'admin'"
                                        @click="confirmDeleteRole(scope.row)"
                                    >
                                        {{ $t('delete') }}
                                    </el-button>
                                </template>
                            </el-table-column>
                        </el-table>
                    </template>
                </DataTable>
                <el-dialog
                    v-model="roleDialogVisible"
                    :title="editingRoleId ? $t('iam.edit_role') : $t('iam.create_role')"
                    destroy-on-close
                    append-to-body
                    width="600px"
                    @close="resetRoleForm"
                >
                    <el-form ref="roleFormRef" :model="roleForm" :rules="roleFormRules" label-position="top">
                        <el-form-item :label="$t('iam.role_name')" prop="name">
                            <el-input v-model="roleForm.name" :disabled="editingRoleId === 'admin'" />
                        </el-form-item>
                        <el-form-item :label="$t('description')" prop="description">
                            <el-input v-model="roleForm.description" type="textarea" :rows="2" />
                        </el-form-item>
                        <el-form-item :label="$t('iam.permissions')">
                            <el-checkbox-group v-model="roleForm.permissions" class="permissions-grid">
                                <el-checkbox
                                    v-for="perm in permissionActions"
                                    :key="perm.key"
                                    :label="perm.key"
                                    class="permission-row"
                                >
                                    {{ perm.key }}
                                </el-checkbox>
                            </el-checkbox-group>
                        </el-form-item>
                    </el-form>
                    <template #footer>
                        <el-button @click="roleDialogVisible = false">{{ $t('cancel') }}</el-button>
                        <el-button type="primary" :loading="roleSaving" @click="submitRoleForm">
                            {{ editingRoleId ? $t('save') : $t('iam.create_role') }}
                        </el-button>
                    </template>
                </el-dialog>
            </el-tab-pane>
            <el-tab-pane :label="$t('iam.bindings')" name="bindings">
                <div class="mb-3">
                    <el-button type="primary" @click="openBindingCreate">{{ $t('iam.add_binding') }}</el-button>
                </div>
                <DataTable striped :total="bindings?.length ?? 0">
                    <template #table>
                        <NoData v-if="!bindings?.length" />
                        <el-table v-else :data="bindings">
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
                            <el-table-column prop="namespace" :label="$t('namespace')">
                                <template #default="scope">
                                    {{ scope.row.namespace ?? $t('iam.all_namespaces') }}
                                </template>
                            </el-table-column>
                            <el-table-column :label="$t('actions')" width="120" fixed="right">
                                <template #default="scope">
                                    <el-button link type="danger" size="small" @click="confirmDeleteBinding(scope.row)">
                                        {{ $t('delete') }}
                                    </el-button>
                                </template>
                            </el-table-column>
                        </el-table>
                    </template>
                </DataTable>
                <el-dialog
                    v-model="bindingDialogVisible"
                    :title="$t('iam.add_binding')"
                    destroy-on-close
                    append-to-body
                    width="450px"
                    @close="resetBindingForm"
                >
                    <el-form ref="bindingFormRef" :model="bindingForm" :rules="bindingFormRules" label-position="top">
                        <el-form-item :label="$t('iam.role')" prop="roleId">
                            <el-select v-model="bindingForm.roleId" :placeholder="$t('iam.select_role')" style="width: 100%">
                                <el-option
                                    v-for="r in roles"
                                    :key="r.id"
                                    :label="r.name"
                                    :value="r.id"
                                />
                            </el-select>
                        </el-form-item>
                        <el-form-item :label="$t('users.username')" prop="userId">
                            <el-select v-model="bindingForm.userId" :placeholder="$t('iam.select_user')" style="width: 100%">
                                <el-option
                                    v-for="u in usersList"
                                    :key="u.id"
                                    :label="u.username"
                                    :value="u.id"
                                />
                            </el-select>
                        </el-form-item>
                        <el-form-item :label="$t('namespace')" prop="namespace">
                            <el-input v-model="bindingForm.namespace" :placeholder="$t('iam.namespace_optional')" />
                        </el-form-item>
                    </el-form>
                    <template #footer>
                        <el-button @click="bindingDialogVisible = false">{{ $t('cancel') }}</el-button>
                        <el-button type="primary" :loading="bindingSaving" @click="submitBindingForm">
                            {{ $t('add') }}
                        </el-button>
                    </template>
                </el-dialog>
            </el-tab-pane>
        </el-tabs>
    </section>
</template>

<script setup lang="ts">
    import {computed, onMounted, ref, watch} from "vue";
    import {useI18n} from "vue-i18n";
    import type {FormInstance, FormRules} from "element-plus";
    import {ElMessage, ElMessageBox} from "element-plus";
    import TopNavBar from "../layout/TopNavBar.vue";
    import useRouteContext from "../../composables/useRouteContext";
    import {useRolesStore, type Role} from "../../stores/roles";
    import {useBindingsStore, type Binding} from "../../stores/bindings";
    import {useUsersStore, type User} from "../../stores/users";
    import DataTable from "../layout/DataTable.vue";
    import NoData from "../layout/NoData.vue";

    const PERMISSIONS = ["FLOW", "EXECUTION", "NAMESPACE", "SECRET", "USER", "ROLE", "BINDING"];
    const ACTIONS = ["CREATE", "READ", "UPDATE", "DELETE"];
    const permissionActions = ref(
        PERMISSIONS.flatMap((p) => ACTIONS.map((a) => ({key: `${p}:${a}`})))
    );

    const {t} = useI18n();
    const rolesStore = useRolesStore();
    const bindingsStore = useBindingsStore();
    const usersStore = useUsersStore();

    const routeInfo = computed(() => ({title: t("iam.title")}));
    useRouteContext(routeInfo);

    const activeTab = ref("roles");
    const roles = ref<Role[]>([]);
    const bindings = ref<Binding[]>([]);
    const usersList = ref<User[]>([]);

    const roleDialogVisible = ref(false);
    const editingRoleId = ref<string | null>(null);
    const roleSaving = ref(false);
    const roleFormRef = ref<FormInstance>();
    const roleForm = ref({name: "", description: "", permissions: [] as string[]});
    const roleFormRules: FormRules = {
        name: [{required: true, message: t("iam.role_name_required"), trigger: "blur"}]
    };

    const bindingDialogVisible = ref(false);
    const bindingSaving = ref(false);
    const bindingFormRef = ref<FormInstance>();
    const bindingForm = ref({roleId: "", userId: "", namespace: ""});
    const bindingFormRules: FormRules = {
        roleId: [{required: true, message: t("iam.role_required"), trigger: "change"}],
        userId: [{required: true, message: t("iam.user_required"), trigger: "change"}]
    };

    function roleNameById(id: string) {
        return roles.value.find((r) => r.id === id)?.name ?? id;
    }

    async function loadRoles() {
        try {
            roles.value = await rolesStore.list();
        } catch {
            roles.value = [];
        }
    }

    async function loadBindings() {
        try {
            bindings.value = await bindingsStore.list();
        } catch {
            bindings.value = [];
        }
    }

    async function loadUsers() {
        try {
            usersList.value = await usersStore.list();
        } catch {
            usersList.value = [];
        }
    }

    function openRoleCreate() {
        editingRoleId.value = null;
        roleForm.value = {name: "", description: "", permissions: []};
        roleDialogVisible.value = true;
    }

    function openRoleEdit(row: Role) {
        editingRoleId.value = row.id;
        roleForm.value = {
            name: row.name,
            description: row.description ?? "",
            permissions: [...(row.permissions ?? [])]
        };
        roleDialogVisible.value = true;
    }

    function resetRoleForm() {
        roleFormRef.value?.resetFields();
        editingRoleId.value = null;
    }

    async function submitRoleForm() {
        if (!roleFormRef.value) return;
        await roleFormRef.value.validate(async (valid) => {
            if (!valid) return;
            roleSaving.value = true;
            try {
                if (editingRoleId.value) {
                    await rolesStore.update(
                        editingRoleId.value,
                        roleForm.value.name,
                        roleForm.value.description ?? "",
                        roleForm.value.permissions
                    );
                } else {
                    await rolesStore.create(
                        roleForm.value.name,
                        roleForm.value.description ?? "",
                        roleForm.value.permissions ?? []
                    );
                }
                roleDialogVisible.value = false;
                await loadRoles();
                if (activeTab.value === "bindings") await loadBindings();
            } catch (e: any) {
                const msg = e?.response?.data?.message ?? e?.message ?? String(e);
                ElMessage.error(t("iam.role_save_error", {msg}) || msg);
            } finally {
                roleSaving.value = false;
            }
        });
    }

    async function confirmDeleteRole(row: Role) {
        if (row.id === "admin") return;
        try {
            await ElMessageBox.confirm(t("delete confirm", {name: row.name}));
            await rolesStore.remove(row.id);
            await loadRoles();
            await loadBindings();
        } catch {
            // cancelled
        }
    }

    function openBindingCreate() {
        bindingForm.value = {roleId: "", userId: "", namespace: ""};
        bindingDialogVisible.value = true;
    }

    function resetBindingForm() {
        bindingFormRef.value?.resetFields();
    }

    async function submitBindingForm() {
        if (!bindingFormRef.value) return;
        await bindingFormRef.value.validate(async (valid) => {
            if (!valid) return;
            bindingSaving.value = true;
            try {
                await bindingsStore.create(
                    bindingForm.value.roleId,
                    bindingForm.value.userId || undefined,
                    undefined,
                    bindingForm.value.namespace || undefined
                );
                bindingDialogVisible.value = false;
                await loadBindings();
            } finally {
                bindingSaving.value = false;
            }
        });
    }

    async function confirmDeleteBinding(row: Binding) {
        try {
            await ElMessageBox.confirm(t("delete confirm", {name: row.id}));
            await bindingsStore.remove(row.id);
            await loadBindings();
        } catch {
            // cancelled
        }
    }

    onMounted(() => {
        loadRoles();
        loadBindings();
        loadUsers();
    });

    watch(activeTab, (tab) => {
        if (tab === "roles") loadRoles();
        if (tab === "bindings") loadBindings();
    });
</script>

<style scoped>
    .iam-tabs :deep(.el-tabs__item) {
        margin-right: 1rem;
    }
    .iam-tabs :deep(.el-tabs__nav-wrap::after) {
        display: none;
    }
    .permissions-grid {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 4px 16px;
        max-height: 240px;
        overflow-y: auto;
    }
    .permission-row {
        display: flex;
        align-items: center;
    }
    .mr-1 { margin-right: 4px; }
    .mb-1 { margin-bottom: 4px; }
</style>
