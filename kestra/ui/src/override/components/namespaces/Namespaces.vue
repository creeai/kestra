<template>
    <Navbar :title="routeInfo.title">
        <template #additional-right>
            <el-button
                type="primary"
                @click="openCreateDialog"
            >
                {{ $t('namespaceForm.create_namespace') }}
            </el-button>
        </template>
    </Navbar>

    <el-dialog
        v-model="createDialogVisible"
        :title="$t('namespaceForm.create_namespace')"
        destroy-on-close
        append-to-body
        width="520px"
        @close="resetCreateForm"
    >
        <el-form ref="createFormRef" :model="createForm" :rules="createFormRules" label-position="top">
            <el-form-item :label="$t('namespaceForm.namespace_id')" prop="namespaceId">
                <el-input
                    v-model="createForm.namespaceId"
                    :placeholder="$t('namespaceForm.namespace_id_placeholder')"
                    @input="createForm.namespaceId = (createForm.namespaceId || '').replace(/[^a-zA-Z0-9._-]/g, '')"
                />
            </el-form-item>
            <el-divider>{{ $t('namespaceForm.assign_access') }}</el-divider>
            <el-form-item :label="$t('iam.role')">
                <el-select v-model="createForm.roleId" :placeholder="$t('iam.select_role')" clearable style="width: 100%">
                    <el-option
                        v-for="r in rolesList"
                        :key="r.id"
                        :label="r.name"
                        :value="r.id"
                    />
                </el-select>
            </el-form-item>
            <el-form-item :label="$t('namespaceForm.users_with_access')">
                <el-select
                    v-model="createForm.userIds"
                    :placeholder="$t('iam.select_user')"
                    multiple
                    clearable
                    style="width: 100%"
                >
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
            <el-button @click="createDialogVisible = false">{{ $t('cancel') }}</el-button>
            <el-button type="primary" :loading="createSaving" @click="submitCreateNamespace">
                {{ $t('namespaceForm.create_namespace') }}
            </el-button>
        </template>
    </el-dialog>

    <el-row class="p-5">
        <div class="d-flex justify-content-between align-items-center mb-3 namespaces-toolbar">
            <span class="text-muted">{{ $t('namespaceForm.create_namespace_hint') }}</span>
            <el-button type="primary" size="default" @click="openCreateDialog">
                {{ $t('namespaceForm.create_namespace') }}
            </el-button>
        </div>
        <KSFilter
            :configuration="namespacesFilter"
            :prefix="'namespaces-list'"
            :tableOptions="{
                chart: {shown: false},
                columns: {shown: false},
                refresh: {shown: false}
            }"
            :searchInputFullWidth="true"
            :buttons="{
                savedFilters: {shown: false},
                tableOptions: {shown: false}
            }"
        />

        <el-col v-if="namespaces.length === 0" class="p-3 namespaces">
            <span>{{ $t("no_namespaces") }}</span>
        </el-col>

        <el-col
            v-for="namespace in namespacesHierarchy"
            :key="namespace.id"
            class="namespaces"
            :class="{system: namespace.id === systemNamespace}"
        >
            <el-tree
                :data="[namespace]"
                defaultExpandAll
                :props="{class: 'tree'}"
                class="h-auto p-2 rounded-full"
            >
                <template #default="{data}">
                    <router-link
                        :to="{
                            name: 'namespaces/update',
                            params: {
                                id: data.id,
                                tab: data.system ? 'blueprints' : 'overview',
                            },
                        }"
                        tag="div"
                        class="node"
                    >
                        <div class="d-flex">
                            <FolderOpenOutline class="me-2 icon" />
                            <span class="pe-3">
                                {{ namespaceLabel(data.label) }}
                            </span>
                            <slot name="description" :namespace="data" />
                            <span v-if="data.system" class="system">
                                {{ $t("system_namespace") }}
                            </span>
                        </div>
                        <el-button size="small">
                            <TextSearch />
                        </el-button>
                    </router-link>
                </template>
            </el-tree>
        </el-col>
    </el-row>
</template>

<script setup lang="ts">
    import {computed, onMounted, Ref, ref, watch} from "vue";
    import {useRoute, useRouter} from "vue-router";
    import useRouteContext from "../../../composables/useRouteContext";
    import useNamespaces, {Namespace} from "../../../composables/useNamespaces";
    import {useI18n} from "vue-i18n";
    import type {FormInstance, FormRules} from "element-plus";
    import {ElMessage} from "element-plus";
    import {useMiscStore} from "override/stores/misc";
    import {useNamespacesStore} from "override/stores/namespaces";
    import {useBindingsStore} from "../../../stores/bindings";
    import {useRolesStore, type Role} from "../../../stores/roles";
    import {useUsersStore, type User} from "../../../stores/users";

    import Navbar from "../../../components/layout/TopNavBar.vue";
    import KSFilter from "../../../components/filter/components/KSFilter.vue";
    import {useNamespacesFilter} from "../../../components/filter/configurations";
    import permission from "../../../models/permission";
    import action from "../../../models/action";

    import useRestoreUrl from "../../../composables/useRestoreUrl";
    import {storageKeys} from "../../../utils/constants";

    import FolderOpenOutline from "vue-material-design-icons/FolderOpenOutline.vue";
    import TextSearch from "vue-material-design-icons/TextSearch.vue";
    import {useAuthStore} from "override/stores/auth";

    const namespacesFilter = useNamespacesFilter();
    const router = useRouter();
    const namespacesStore = useNamespacesStore();
    const bindingsStore = useBindingsStore();
    const rolesStore = useRolesStore();
    const usersStore = useUsersStore();
    const {saveRestoreUrl} = useRestoreUrl({restoreUrl: true});

    interface Node {
        id: string;
        label: string;
        description?: string;
        disabled?: boolean;
        children?: Node[];
        system?: boolean;
    }

    const route = useRoute();

    const {t} = useI18n({useScope: "global"});

    const routeInfo = computed(() => ({title: t("namespaces")}));
    useRouteContext(routeInfo);


    const authStore = useAuthStore();
    const canCreate = computed(() => {
        return authStore.user?.hasAnyAction(permission.NAMESPACE, action.CREATE);
    });

    const namespaces = ref([]) as Ref<Namespace[]>;

    function getCreatedNamespaceIds(): string[] {
        try {
            const raw = localStorage.getItem(storageKeys.CREATED_NAMESPACES_IDS);
            if (!raw) return [];
            const parsed = JSON.parse(raw);
            return Array.isArray(parsed) ? parsed : [];
        } catch {
            return [];
        }
    }

    function addCreatedNamespaceId(id: string) {
        const ids = getCreatedNamespaceIds();
        if (ids.includes(id)) return;
        ids.push(id);
        localStorage.setItem(storageKeys.CREATED_NAMESPACES_IDS, JSON.stringify(ids));
    }

    const loadData = async (extraForceIncludeIds?: string[]) => {
        const options: Record<string, unknown> = route.query?.q !== undefined ? {q: route.query.q} : {};
        const knownCreated = getCreatedNamespaceIds();
        const forceIncludeIds = extraForceIncludeIds?.length
            ? [...new Set([...knownCreated, ...extraForceIncludeIds])]
            : knownCreated.length ? knownCreated : undefined;
        if (forceIncludeIds?.length) options.forceIncludeIds = forceIncludeIds;
        namespaces.value = await useNamespaces(1000, options).all();
    };

    const createDialogVisible = ref(false);
    const createSaving = ref(false);
    const createFormRef = ref<FormInstance>();
    const createForm = ref({namespaceId: "", roleId: "", userIds: [] as string[]});
    const createFormRules: FormRules = {
        namespaceId: [
            {required: true, message: t("namespaceForm.namespace_id_required"), trigger: "blur"},
            {pattern: /^[a-zA-Z0-9._-]+$/, message: t("namespaceForm.namespace_id_pattern"), trigger: "blur"}
        ]
    };
    const rolesList = ref<Role[]>([]);
    const usersList = ref<User[]>([]);

    async function loadRolesAndUsers() {
        try {
            rolesList.value = await rolesStore.list();
            usersList.value = await usersStore.list();
        } catch {
            rolesList.value = [];
            usersList.value = [];
        }
    }

    function openCreateDialog() {
        createForm.value = {namespaceId: "", roleId: "", userIds: []};
        loadRolesAndUsers();
        createDialogVisible.value = true;
    }

    function resetCreateForm() {
        createFormRef.value?.resetFields();
    }

    async function submitCreateNamespace() {
        if (!createFormRef.value) return;
        await createFormRef.value.validate(async (valid) => {
            if (!valid) return;
            const id = (createForm.value.namespaceId || "").trim();
            if (!id) return;
            createSaving.value = true;
            try {
                await namespacesStore.createDirectory({namespace: id, path: "/"});
                addCreatedNamespaceId(id);
                if (createForm.value.roleId && createForm.value.userIds?.length) {
                    for (const userId of createForm.value.userIds) {
                        await bindingsStore.create(
                            createForm.value.roleId,
                            userId,
                            undefined,
                            id
                        );
                    }
                }
                createDialogVisible.value = false;
                await loadData([id]);
                await router.push({name: "namespaces/update", params: {id, tab: "overview"}});
                ElMessage.success(t("namespaceForm.created_success"));
            } catch (err: any) {
                ElMessage.error(err?.response?.data?.message || err?.message || t("error"));
            } finally {
                createSaving.value = false;
            }
        });
    }

    onMounted(() => loadData());
    watch(
        () => route.query.q,
        () => {
            loadData();
            saveRestoreUrl();
        },
        {immediate: true}
    );

    const miscStore = useMiscStore();
    const systemNamespace = computed(
        () => miscStore.configs?.systemNamespace || "system",
    );
    
    const namespacesHierarchy = computed(() => {
        if (namespaces.value === undefined || namespaces.value.length === 0) {
            return [];
        }

        const map = {} as Node[];

        namespaces.value.forEach((item) => {
            const parts = item.id.split(".");
            let currentLevel = map as any;

            parts.forEach((_part, index) => {
                const label = parts.slice(0, index + 1).join(".");
                const isLeaf = index === parts.length - 1;

                if (!currentLevel[label])
                    currentLevel[label] = {
                        id: label,
                        label,
                        description: isLeaf ? item.description : undefined,
                        children: [],
                    };
                currentLevel = currentLevel[label].children;
            });
        });

        const build = (nodes: Node[]): Node[] => {
            return Object.values(nodes).map((node) => {
                const result: Node = {
                    id: node.id,
                    label: node.label,
                    description: node.description,
                    children: node.children ? build(node.children) : undefined,
                };
                return result;
            });
        };

        const result = build(map);

        const system = result.findIndex(
            (namespace) => namespace.id === systemNamespace.value,
        );

        if (system !== -1) {
            const [systemItem] = result.splice(system, 1);
            result.unshift({...systemItem, system: true});
        }

        return result;
    });

    const namespaceLabel = (path: string) => {
        const segments = path.split(".");
        return segments.length > 1 ? segments[segments.length - 1] : path;
    };
</script>

<style scoped lang="scss">
@import "@kestra-io/ui-libs/src/scss/color-palette.scss";

.namespaces-toolbar {
    padding: 0.5rem 0;
    border-bottom: 1px solid var(--ks-border-primary);
    margin-bottom: 0.75rem;
}

.namespaces {
    margin: 0.25rem 0;
    border-radius: var(--bs-border-radius-lg);
    border: 1px solid var(--ks-border-primary);
    box-shadow: 0px 2px 4px 0px var(--ks-card-shadow);

    &.system {
        border-color: $base-blue-300;

        & span.system {
            line-height: 1.5rem;
            font-size: var(--font-size-xs);
            color: var(--ks-content-primary);
        }
    }

    .rounded-full {
        border-radius: var(--bs-border-radius-lg);
        background-color: var(--ks-background-card)
    }

    :deep(.el-tree-node__content) {
        height: 2.25rem;
        overflow: hidden;
        background: transparent;
        border-radius: var(--bs-border-radius-lg);

        &:hover {
            background: var(--ks-background-body);
        }

        .icon {
            color: var(--ks-content-link);
        }
    }

    .node {
        flex: 1;
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 0 0.5rem;
        color: var(--ks-content-primary);

        &:hover {
            background: transparent;
            color: var(--ks-content-link);
        }
    }
}
</style>
