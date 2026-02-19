<template>
    <section class="container">
        <div v-if="settingsNotAvailable" class="p-4 text-muted">
            {{ $t('namespaceForm.settings_not_available') }}
        </div>
        <template v-else>
            <div class="mb-3">
                <el-button type="primary" :loading="saving" @click="submit">{{ $t('save') }}</el-button>
            </div>
            <el-input
                v-model="yamlText"
                type="textarea"
                :rows="16"
                :placeholder="$t('pluginDefaults.placeholder')"
                class="font-monospace"
            />
            <p class="form-hint mt-2">
                {{ $t('namespaceForm.plugin_defaults_hint') }}
            </p>
        </template>
    </section>
</template>

<script setup lang="ts">
    import {onMounted, ref, watch} from "vue";
    import {useBaseNamespacesStore} from "../../../composables/useBaseNamespaces";
    import yaml from "yaml";

    const props = defineProps<{ namespace: string }>();

    const namespacesStore = useBaseNamespacesStore();
    const saving = ref(false);
    const settingsNotAvailable = ref(false);
    const yamlText = ref("");

    async function load() {
        if (!props.namespace) return;
        try {
            const data = await namespacesStore.loadInheritedPluginDefaults({ id: props.namespace, commit: true });
            if (Array.isArray(data) && data.length > 0) {
                yamlText.value = yaml.stringify(data, { lineWidth: 0 });
            } else {
                yamlText.value = "";
            }
        } catch (e: any) {
            if (e?.response?.status === 501) {
                settingsNotAvailable.value = true;
            }
        }
    }

    async function submit() {
        let parsed: any[] = [];
        const trimmed = yamlText.value.trim();
        if (trimmed) {
            try {
                parsed = yaml.parse(trimmed);
                if (!Array.isArray(parsed)) {
                    parsed = [parsed];
                }
            } catch {
                return;
            }
        }
        saving.value = true;
        try {
            await namespacesStore.setNamespacePluginDefaults({ namespace: props.namespace, defaults: parsed });
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
    .font-monospace {
        font-family: var(--el-font-family-monospace);
    }
</style>
