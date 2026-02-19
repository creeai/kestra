<template>
    <template v-if="hasAuditAccess">
        <TopNavBar :title="routeInfo.title" />
        <section class="container padding-bottom">
            <DataTable striped :total="total">
                <template #table>
                    <NoData v-if="!logs?.length && !loading" />
                    <el-table v-else v-loading="loading" :data="logs">
                        <el-table-column prop="timestamp" :label="$t('audit.timestamp')" min-width="160">
                            <template #default="scope">
                                {{ formatDate(scope.row.timestamp) }}
                            </template>
                        </el-table-column>
                        <el-table-column prop="actorId" :label="$t('audit.actor')" width="140" />
                        <el-table-column prop="action" :label="$t('audit.action')" width="100" />
                        <el-table-column prop="resourceType" :label="$t('audit.resourceType')" width="120" />
                        <el-table-column prop="resourceId" :label="$t('audit.resourceId')" min-width="120" />
                        <el-table-column prop="namespace" :label="$t('audit.namespace')" width="140" />
                    </el-table>
                </template>
            </DataTable>
        </section>
    </template>
    <Layout
        v-else
        :title="$t('demos.audit-logs.title')"
        :image="{ source: sourceImg, alt: $t('demos.audit-logs.title') }"
        :video="{ source: 'https://www.youtube.com/embed/Qz24gBPGZHs' }"
    >
        <template #message>
            {{ $t('demos.audit-logs.message') }}
        </template>
    </Layout>
</template>

<script setup lang="ts">
    import { computed, onMounted, ref } from "vue";
    import { useI18n } from "vue-i18n";
    import Layout from "../../../components/demo/Layout.vue";
    import TopNavBar from "../../../components/layout/TopNavBar.vue";
    import DataTable from "../../../components/layout/DataTable.vue";
    import NoData from "../../../components/layout/NoData.vue";
    import sourceImg from "../../../assets/demo/audit-logs.png";
    import useRouteContext from "../../../composables/useRouteContext";
    import { useAuditLogsStore, type AuditLogEntry } from "../../../stores/auditLogs";

    const auditLogsStore = useAuditLogsStore();
    const { t } = useI18n();

    const loading = ref(true);
    const hasAuditAccess = ref(false);
    const logs = ref<AuditLogEntry[]>([]);
    const total = ref(0);

    const routeInfo = computed(() => ({ title: t("auditlogs") }));

    useRouteContext(routeInfo);

    onMounted(async () => {
        try {
            const res = await auditLogsStore.list({ page: 1, size: 25, sort: "timestamp:desc" });
            hasAuditAccess.value = true;
            logs.value = res.results ?? [];
            total.value = res.total ?? 0;
        } catch {
            hasAuditAccess.value = false;
        } finally {
            loading.value = false;
        }
    });

    function formatDate(ts: string | undefined) {
        if (!ts) return "";
        try {
            const d = new Date(ts);
            return d.toLocaleString();
        } catch {
            return ts;
        }
    }
</script>
