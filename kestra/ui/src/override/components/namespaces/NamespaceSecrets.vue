<template>
    <section class="d-flex flex-column fill-height container">
        <div class="mb-3">
            <el-button type="primary" @click="addSecretModalVisible = true">
                {{ $t('secret.add') }}
            </el-button>
        </div>
        <SecretsTable
            filterable
            :namespace="props.namespace"
            :addSecretModalVisible="addSecretModalVisible"
            @update:add-secret-modal-visible="addSecretModalVisible = $event"
        />
    </section>
</template>

<script setup lang="ts">
    import {ref} from "vue";
    import SecretsTable from "../../../components/secrets/SecretsTable.vue";

    const props = defineProps<{
        namespace: string;
    }>();

    const addSecretModalVisible = ref(false);
</script>
