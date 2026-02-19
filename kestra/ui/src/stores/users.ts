import {defineStore} from "pinia";
import {apiUrlWithoutTenants} from "override/utils/route";
import {ref} from "vue";
import {useAxios} from "../utils/axios";

export interface User {
    id: string;
    username: string;
    disabled: boolean;
    createdAt?: string;
}

export const useUsersStore = defineStore("users", () => {
    const axios = useAxios();

    async function list(): Promise<User[]> {
        const response = await axios.get(`${apiUrlWithoutTenants()}/users`);
        return response.data;
    }

    async function get(id: string): Promise<User | null> {
        try {
            const response = await axios.get(`${apiUrlWithoutTenants()}/users/${id}`);
            return response.data;
        } catch {
            return null;
        }
    }

    async function create(username: string, password: string, disabled: boolean): Promise<User> {
        const response = await axios.post(`${apiUrlWithoutTenants()}/users`, {
            username,
            password,
            disabled
        });
        return response.data;
    }

    async function update(id: string, username: string, disabled: boolean, password?: string): Promise<User> {
        const body: { username: string; disabled: boolean; password?: string } = { username, disabled };
        if (password != null && password !== "") {
            body.password = password;
        }
        const response = await axios.patch(`${apiUrlWithoutTenants()}/users/${id}`, body);
        return response.data;
    }

    async function remove(id: string): Promise<void> {
        await axios.delete(`${apiUrlWithoutTenants()}/users/${id}`);
    }

    async function resetPassword(id: string, newPassword: string): Promise<void> {
        await axios.post(`${apiUrlWithoutTenants()}/users/${id}/reset-password`, { newPassword });
    }

    return {
        list,
        get,
        create,
        update,
        remove,
        resetPassword
    };
});
