import {defineStore} from "pinia";
import {apiUrlWithoutTenants} from "override/utils/route";
import {ref} from "vue";
import {useAxios} from "../utils/axios";

export interface Binding {
    id: string;
    roleId: string;
    userId?: string;
    groupId?: string;
    namespace?: string;
}

export const useBindingsStore = defineStore("bindings", () => {
    const axios = useAxios();

    async function list(params?: { userId?: string; roleId?: string; namespace?: string }): Promise<Binding[]> {
        const searchParams = new URLSearchParams();
        if (params?.userId) searchParams.set("userId", params.userId);
        if (params?.roleId) searchParams.set("roleId", params.roleId);
        if (params?.namespace) searchParams.set("namespace", params.namespace);
        const query = searchParams.toString();
        const url = query ? `${apiUrlWithoutTenants()}/bindings?${query}` : `${apiUrlWithoutTenants()}/bindings`;
        const response = await axios.get(url);
        return response.data;
    }

    async function get(id: string): Promise<Binding | null> {
        try {
            const response = await axios.get(`${apiUrlWithoutTenants()}/bindings/${id}`);
            return response.data;
        } catch {
            return null;
        }
    }

    async function create(roleId: string, userId?: string, groupId?: string, namespace?: string): Promise<Binding> {
        const response = await axios.post(`${apiUrlWithoutTenants()}/bindings`, {
            roleId,
            userId: userId || null,
            groupId: groupId || null,
            namespace: namespace || null
        });
        return response.data;
    }

    async function remove(id: string): Promise<void> {
        await axios.delete(`${apiUrlWithoutTenants()}/bindings/${id}`);
    }

    return {
        list,
        get,
        create,
        remove
    };
});
