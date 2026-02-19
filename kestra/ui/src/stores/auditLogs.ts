import axios from "axios";
import { defineStore } from "pinia";
import { apiUrlWithoutTenants } from "override/utils/route";

export interface AuditLogEntry {
    id: string;
    timestamp: string;
    actorId: string;
    actorType?: string;
    action: string;
    resourceType: string;
    resourceId?: string;
    namespace?: string;
    tenantId?: string;
    details?: Record<string, unknown>;
}

export interface AuditLogsResponse {
    results: AuditLogEntry[];
    total: number;
}

export const useAuditLogsStore = defineStore("auditLogs", () => {
    async function list(params: {
        page?: number;
        size?: number;
        sort?: string;
        from?: string;
        to?: string;
        actorId?: string;
        resourceType?: string;
        resourceId?: string;
        namespace?: string;
    }) {
        const { data } = await axios.get<AuditLogsResponse>(
            `${apiUrlWithoutTenants()}/audit-logs`,
            { withCredentials: true, params }
        );
        return data;
    }

    return { list };
});
