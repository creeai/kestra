import {defineStore} from "pinia";
import {apiUrlWithoutTenants} from "override/utils/route";
import {ref} from "vue";
import {useAxios} from "../utils/axios";

export interface Role {
    id: string;
    name: string;
    description?: string;
    permissions: string[];
}

export const useRolesStore = defineStore("roles", () => {
    const axios = useAxios();

    async function list(): Promise<Role[]> {
        const response = await axios.get(`${apiUrlWithoutTenants()}/roles`);
        return response.data;
    }

    async function get(id: string): Promise<Role | null> {
        try {
            const response = await axios.get(`${apiUrlWithoutTenants()}/roles/${id}`);
            return response.data;
        } catch {
            return null;
        }
    }

    async function create(name: string, description: string, permissions: string[]): Promise<Role> {
        const response = await axios.post(`${apiUrlWithoutTenants()}/roles`, {
            name,
            description,
            permissions
        });
        return response.data;
    }

    async function update(id: string, name: string, description: string, permissions: string[]): Promise<Role> {
        const response = await axios.patch(`${apiUrlWithoutTenants()}/roles/${id}`, {
            name,
            description,
            permissions
        });
        return response.data;
    }

    async function remove(id: string): Promise<void> {
        await axios.delete(`${apiUrlWithoutTenants()}/roles/${id}`);
    }

    return {
        list,
        get,
        create,
        update,
        remove
    };
});
