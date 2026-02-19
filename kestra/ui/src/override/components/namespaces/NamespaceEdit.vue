<template>
    <section class="container">
        <div v-if="settingsNotAvailable" class="p-4 text-muted">
            {{ $t('namespaceForm.settings_not_available') }}
        </div>
        <el-form
            v-else
            ref="formRef"
            :model="form"
            label-position="top"
            class="namespace-edit-form"
            style="max-width: 600px"
        >
            <el-form-item :label="$t('namespaceForm.description')">
                <el-input
                    v-model="form.description"
                    type="textarea"
                    :rows="3"
                    :placeholder="$t('namespaceForm.description_placeholder')"
                />
            </el-form-item>
            <el-form-item :label="$t('namespaceForm.allowed_namespaces')">
                <el-select
                    v-model="form.allowedNamespaces"
                    multiple
                    filterable
                    allow-create
                    default-first-option
                    :placeholder="$t('namespaceForm.allowed_namespaces_placeholder')"
                    style="width: 100%"
                />
                <div class="form-hint mt-1">
                    {{ $t('namespaceForm.allowed_namespaces_hint') }}
                </div>
            </el-form-item>
            <el-form-item>
                <el-button type="primary" :loading="saving" @click="submit">
                    {{ $t('save') }}
                </el-button>
            </el-form-item>
        </el-form>
    </section>
</template>

<script setup lang="ts">
    import {onMounted, ref, watch} from "vue";
    import {useI18n} from "vue-i18n";
    import type {FormInstance} from "element-plus";
    import {useBaseNamespacesStore} from "../../../composables/useBaseNamespaces";

    const props = defineProps<{ namespace: string }>();

    const {t} = useI18n();
    const namespacesStore = useBaseNamespacesStore();
    const formRef = ref<FormInstance>();
    const form = ref<{ description: string; allowedNamespaces: string[] }>({
        description: "",
        allowedNamespaces: [],
    });
    const saving = ref(false);
    const settingsNotAvailable = ref(false);

    async function load() {
        if (!props.namespace) return;
        try {
            const data = await namespacesStore.loadNamespaceSettings(props.namespace);
            if (data) {
                form.value.description = data.description ?? "";
                form.value.allowedNamespaces = Array.isArray(data.allowedNamespaces) ? [...data.allowedNamespaces] : [];
            }
        } catch (e: any) {
            if (e?.response?.status === 501) {
                settingsNotAvailable.value = true;
            }
        }
    }

    async function submit() {
        saving.value = true;
        try {
            await namespacesStore.updateNamespaceSettings({
                namespace: props.namespace,
                description: form.value.description || undefined,
                allowedNamespaces: form.value.allowedNamespaces.length > 0 ? form.value.allowedNamespaces : undefined,
            });
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
