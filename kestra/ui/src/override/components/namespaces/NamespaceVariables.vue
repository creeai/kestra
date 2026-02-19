<template>
    <section class="container">
        <div v-if="settingsNotAvailable" class="p-4 text-muted">
            {{ $t('namespaceForm.settings_not_available') }}
        </div>
        <template v-else>
            <div class="mb-3">
                <el-button type="primary" @click="addRow">{{ $t('namespaceForm.var_add') }}</el-button>
                <el-button type="primary" :loading="saving" @click="submit">{{ $t('save') }}</el-button>
            </div>
            <el-table v-loading="loading" :data="rows" stripe>
                <el-table-column :label="$t('namespaceForm.var_key')" min-width="180">
                    <template #default="scope">
                        <el-input v-model="scope.row.key" :placeholder="$t('namespaceForm.var_key_placeholder')" />
                    </template>
                </el-table-column>
                <el-table-column :label="$t('namespaceForm.var_value')" min-width="240">
                    <template #default="scope">
                        <el-input v-model="scope.row.value" :placeholder="$t('namespaceForm.var_value_placeholder')" type="textarea" :autosize="{ minRows: 1 }" />
                    </template>
                </el-table-column>
                <el-table-column width="80" fixed="right">
                    <template #default="scope">
                        <el-button link type="danger" size="small" @click="removeRow(scope.$index)">
                            {{ $t('delete') }}
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>
            <p class="form-hint mt-2">
                {{ $t('namespaceForm.variables_hint') }}
            </p>
        </template>
    </section>
</template>

<script setup lang="ts">
    import {onMounted, ref, watch} from "vue";
    import {useBaseNamespacesStore} from "../../../composables/useBaseNamespaces";

    const props = defineProps<{ namespace: string }>();

    const namespacesStore = useBaseNamespacesStore();
    const loading = ref(false);
    const saving = ref(false);
    const settingsNotAvailable = ref(false);
    const rows = ref<{ key: string; value: string }[]>([]);

    function addRow() {
        rows.value.push({ key: "", value: "" });
    }

    function removeRow(index: number) {
        rows.value.splice(index, 1);
    }

    async function load() {
        if (!props.namespace) return;
        loading.value = true;
        try {
            const data = await namespacesStore.loadInheritedVariables({ id: props.namespace, commit: true });
            rows.value = (data ?? []).map((r: { key: string; value: string }) => ({ key: r.key, value: r.value ?? "" }));
            if (rows.value.length === 0) {
                rows.value = [{ key: "", value: "" }];
            }
        } catch (e: any) {
            if (e?.response?.status === 501) {
                settingsNotAvailable.value = true;
            }
        } finally {
            loading.value = false;
        }
    }

    async function submit() {
        const variables: Record<string, string> = {};
        for (const row of rows.value) {
            if (row.key != null && String(row.key).trim()) {
                variables[String(row.key).trim()] = row.value != null ? String(row.value) : "";
            }
        }
        saving.value = true;
        try {
            await namespacesStore.setNamespaceVariables({ namespace: props.namespace, variables });
        } finally {
            saving.value = false;
        }
    }

    onMounted(() => load());
    watch(() => props.namespace, () => load());
</script>

<style scoped>
    .form-hint {
        font-size: 0.85rem;
        color: var(--el-text-color-secondary);
    }
</style>
