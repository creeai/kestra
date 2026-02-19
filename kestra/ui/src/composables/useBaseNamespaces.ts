import {ref} from "vue";
import {useRouter} from "vue-router";
import {apiUrl, apiUrlWithTenant} from "override/utils/route";
import Utils from "../utils/utils";
import {useAxios} from "../utils/axios";

function base(namespace: string) {
    return `${apiUrl()}/namespaces/${namespace}`;
}

const HEADERS = {headers: {"Content-Type": "multipart/form-data"}};
const slashPrefix = (path: string) => (path.startsWith("/") ? path : `/${path}`);
const safePath = (path: string) => encodeURIComponent(path).replace(/%2C|%2F/g, "/");
export const VALIDATE = {validateStatus: (status: number) => status === 200 || status === 404};

export const useBaseNamespacesStore = () => {
    const namespace = ref<any>(undefined);
    const namespaces = ref<any[] | undefined>(undefined);
    const secrets = ref<any[] | undefined>(undefined);
    const inheritedSecrets = ref<any>(undefined);
    const kvs = ref<any[] | undefined>(undefined);
    const inheritedKVs = ref<any>(undefined);
    const inheritedKVModalVisible = ref(false);
    const addKvModalVisible = ref(false);
    const autocomplete = ref<any>(undefined);
    const total = ref(0);
    const existing = ref(true);
    const namespaceSettings = ref<{ description?: string; allowedNamespaces?: string[] } | undefined>(undefined);
    const inheritedVariables = ref<{ key: string; value: string }[]>([]);
    const inheritedPluginDefaults = ref<any[]>([]);

    const axios = useAxios();
    const router = useRouter();

    async function loadAutocomplete(this: any, options?: {q?: string, ids?: string[], existingOnly?: boolean}) {
        const response = await axios.post(`${apiUrlWithTenant(router.currentRoute.value)}/namespaces/autocomplete`, options ?? {});
        autocomplete.value = response.data;
        return response.data;
    }

    async function search(this: any, options: any) {
        const shouldCommit = options.commit !== false;
        delete options.commit;
        const response = await axios.get(`${apiUrl()}/namespaces/search`, {params: options, ...VALIDATE});
        if (response.status === 200 && shouldCommit) {
            namespaces.value = response.data.results;
            total.value = response.data.total;
        }
        return response.data;
    }

    async function load(this: any, id: string) {
        const response = await axios.get(`${apiUrl()}/namespaces/${id}`, VALIDATE);

        if(response.status === 200) {
            namespace.value = response.data;
            existing.value = true;
        }

        if(response.status === 404) {
            existing.value = false;
        }

        return response.data;
    }

    async function update(this: any, _: {route: any, payload: any}) {
        // NOOP IN OSS (namespace settings use updateNamespaceSettings)
    }

    async function loadNamespaceSettings(this: any, namespace: string) {
        const response = await axios.get(`${apiUrl()}/namespaces/${namespace}/settings`, VALIDATE);
        if (response.status === 200) {
            namespaceSettings.value = {
                description: response.data?.description ?? undefined,
                allowedNamespaces: response.data?.allowedNamespaces ?? undefined,
            };
        } else {
            namespaceSettings.value = undefined;
        }
        return namespaceSettings.value;
    }

    async function updateNamespaceSettings(this: any, payload: { namespace: string; description?: string; allowedNamespaces?: string[] }) {
        await axios.patch(`${apiUrl()}/namespaces/${payload.namespace}/settings`, {
            description: payload.description,
            allowedNamespaces: payload.allowedNamespaces,
        });
        namespaceSettings.value = { description: payload.description, allowedNamespaces: payload.allowedNamespaces };
    }

    async function loadDependencies(this: any, options: {namespace: string}) {
        return await axios.get(`${apiUrl()}/namespaces/${options.namespace}/dependencies`);
    }

    async function kvsList(this: any, item: {id: string}) {
        const response = await axios.get(`${apiUrl()}/namespaces/${item.id}/kv`, VALIDATE);
        kvs.value = response.data;
        return response.data;
    }

    async function kv(this: any, payload: {namespace: string; key: string}) {
        const response = await axios.get(`${apiUrl()}/namespaces/${payload.namespace}/kv/${payload.key}`, VALIDATE);
        if (response.status === 404) {
            throw new Error(response.data.message);
        }
        const data = response.data;
        const contentLength = response.headers?.["content-length"];

        if (contentLength === (data.length + 2).toString()) {
            return `"${data}"`;
        }
        return data;
    }

    async function loadInheritedKVs(this: any, id: string) {
        const response = await axios.get(`${apiUrl()}/namespaces/${id}/kv/inheritance`, {...VALIDATE});
        inheritedKVs.value = response.data;
    }

    async function createKv(this: any, payload: {namespace: string; key: string; value: any; contentType: string; description: string; ttl?: string}) {
        await axios.put(
            `${apiUrl()}/namespaces/${payload.namespace}/kv/${payload.key}`,
            payload.value,
            {
                headers: {
                    "Content-Type": payload.contentType,
                    "description": payload.description,
                    "ttl": payload.ttl
                }
            }
        );
        return kvsList.call(this, {id: payload.namespace});
    }

    async function deleteKv(this: any, payload: {namespace: string; key: string}) {
        await axios.delete(`${apiUrl()}/namespaces/${payload.namespace}/kv/${payload.key}`);
        return kvsList.call(this, {id: payload.namespace});
    }

    async function deleteKvs(this: any, payload: {namespace: string; request: any}) {
        await axios.delete(`${apiUrl()}/namespaces/${payload.namespace}/kv`, {
            data: payload.request
        });
        return kvsList.call(this, {id: payload.namespace});
    }

    async function loadInheritedSecrets(this: any, {id, commit: shouldCommit, ...params}: {id: string; commit: boolean | undefined; [key: string]: any}): Promise<Record<string, string[]>> {
        const response = await axios.get(`${apiUrl()}/namespaces/${id}/inherited-secrets`, {
            ...VALIDATE,
            params
        });
        if (shouldCommit !== false) {
            inheritedSecrets.value = response.data;
        }
        if (response.status === 404) {
            return {[id]: []}
        }
        return response.data;
    }

    async function listSecrets(this: any, {id, commit: shouldCommit, ...params}: {id: string; commit: boolean | undefined; [key: string]: any}): Promise<{total: number, results: {key: string, description?: string, tags?: Record<string, string>}[], readOnly?: boolean}> {
        const response = await axios.get(`${apiUrl()}/namespaces/${id}/secrets`, {
            ...VALIDATE,
            params: {
                ...params
            }
        });
        if (response.status === 200 && shouldCommit !== false) {
            secrets.value = response.data.results;
        }
        if (response.status === 404) {
            return {total: 0, results: [], readOnly: false};
        }
        return response.data;
    }

    async function usableSecrets(this: ReturnType<typeof useBaseNamespacesStore>, id: string): Promise<string[]> {
        return [
            ...Object.values((await this.loadInheritedSecrets({id, commit: false})) ?? {}).flat(),
            ...(await this.listSecrets({id, commit: false})).results.map(({key}) => key)
        ];
    }

    async function createSecrets(this: any, payload: {namespace: string; secret: any}) {
        await axios.post(`${apiUrl()}/namespaces/${payload.namespace}/secrets`, {
            key: payload.secret.key,
            value: payload.secret.value,
            description: payload.secret.description,
            tags: payload.secret.tags
        });
    }

    async function patchSecret(this: any, payload: {namespace: string; secret: any}) {
        await axios.patch(
            `${apiUrl()}/namespaces/${payload.namespace}/secrets/${encodeURIComponent(payload.secret.key)}`,
            {
                value: payload.secret.value,
                description: payload.secret.description,
                tags: payload.secret.tags
            }
        );
    }

    async function deleteSecrets(this: any, payload: {namespace: string; key: string}) {
        await axios.delete(`${apiUrl()}/namespaces/${payload.namespace}/secrets/${encodeURIComponent(payload.key)}`);
    }

    async function loadInheritedVariables(this: any, {id, commit: shouldCommit}: {id: string, commit?: boolean}) {
        try {
            const response = await axios.get(`${apiUrl()}/namespaces/${id}/variables`, VALIDATE);
            if (response.status === 200 && shouldCommit !== false) {
                inheritedVariables.value = Array.isArray(response.data) ? response.data : [];
            }
        } catch {
            inheritedVariables.value = [];
        }
        return inheritedVariables.value;
    }

    async function setNamespaceVariables(this: any, payload: { namespace: string; variables: Record<string, string> }) {
        await axios.put(`${apiUrl()}/namespaces/${payload.namespace}/variables`, payload.variables);
        await loadInheritedVariables.call(this, {id: payload.namespace, commit: true});
    }

    async function loadInheritedPluginDefaults(this: any, {id, commit: shouldCommit}: {id: string, commit?: boolean}) {
        try {
            const response = await axios.get(`${apiUrl()}/namespaces/${id}/plugin-defaults`, VALIDATE);
            if (response.status === 200 && shouldCommit !== false) {
                inheritedPluginDefaults.value = Array.isArray(response.data) ? response.data : [];
            }
        } catch {
            inheritedPluginDefaults.value = [];
        }
        return inheritedPluginDefaults.value;
    }

    async function setNamespacePluginDefaults(this: any, payload: { namespace: string; defaults: any[] }) {
        await axios.put(`${apiUrl()}/namespaces/${payload.namespace}/plugin-defaults`, payload.defaults);
        await loadInheritedPluginDefaults.call(this, {id: payload.namespace, commit: true});
    }

    async function createDirectory(this: any, payload: {namespace: string; path: string}) {
        const URL = `${base(payload.namespace)}/files/directory?path=${slashPrefix(payload.path)}`;
        await axios.post(URL);
    }

    async function readDirectory<T>(this: any, payload: {namespace: string; path?: string}): Promise<T[]> {
        const URL = `${base(payload.namespace)}/files/directory${payload.path ? `?path=${slashPrefix(safePath(payload.path))}` : ""}`;
        // Accept 200 or 404 so axios doesn't treat 404 as an error (which would set coreStore.error globally)
        const response = await axios.get(URL, VALIDATE);

        // If directory not found, mimic previous behavior (throw) without triggering global 404 page
        if (response.status === 404) {
            const notFoundError: any = new Error("Directory not found");
            notFoundError.status = 404;
            throw notFoundError;
        }

        return response.data ?? [];
    }

    async function createFile(this: any, payload: {namespace: string; path: string; content: string}) {
        const DATA = new FormData();
        const BLOB = new Blob([payload.content], {type: "text/plain"});
        DATA.append("fileContent", BLOB);

        const URL = `${base(payload.namespace)}/files?path=${slashPrefix(payload.path)}`;
        await axios.post(URL, Utils.toFormData(DATA), HEADERS);
    }

    async function fileRevisions(this: any, payload: {namespace: string; path: string}): Promise<{revision: number}[]> {
        if (!payload.path) return [];

        const URL = `${base(payload.namespace)}/files/revisions?path=${slashPrefix(safePath(payload.path))}`;
        const request = await axios.get(URL, {
            ...VALIDATE
        });

        if(request.status === 404) {
            const message = JSON.parse(request.data)?.message;
            console.error(message ?? "File not found");
            return [];
        }

        return (request.data as {revision: number}[]);
    }

    async function readFile(this: any, payload: {namespace: string; path: string, revision?: number}): Promise<{content?: string, notFound?: boolean, error?: string}> {
        if (!payload.path) return {error: "Path is required"};

        const URL = `${base(payload.namespace)}/files?path=${slashPrefix(safePath(payload.path))}${payload.revision !== undefined ? `&revision=${payload.revision}` : ""}`;
        const request = await axios.get<string>(URL, {
            ...VALIDATE,
            transformResponse: (response: any) => response,
            responseType: "json"
        });

        if(request.status === 404) {
            const message = JSON.parse(request.data)?.message;
            return {notFound: true, error: message ?? "File not found"};
        }

        return {content: request.data ?? ""};
    }

    async function searchFiles(this: any, payload: {namespace: string; query: string}) {
        const URL = `${base(payload.namespace)}/files/search?q=${payload.query}`;
        const request = await axios.get(URL);
        return request.data ?? [];
    }

    async function importFileDirectory(this: any, payload: {namespace: string; path: string; content: ArrayBuffer}) {
        const DATA = new FormData();
        const BLOB = new Blob([payload.content], {type: "text/plain"});
        DATA.append("fileContent", BLOB);

        const URL = `${base(payload.namespace)}/files?path=${slashPrefix(safePath(payload.path))}`;
        await axios.post(URL, DATA, HEADERS);
    }

    async function moveFileDirectory(this: any, payload: {namespace: string; old: string; new: string}) {
        const URL = `${base(payload.namespace)}/files?from=${slashPrefix(payload.old)}&to=${slashPrefix(payload.new)}`;
        await axios.put(URL);
    }

    async function renameFileDirectory(this: any, payload: {namespace: string; old: string; new: string}) {
        const URL = `${base(payload.namespace)}/files?from=${slashPrefix(payload.old)}&to=${slashPrefix(payload.new)}`;
        await axios.put(URL);
    }

    async function deleteFileDirectory(this: any, payload: {namespace: string; path: string}) {
        const URL = `${base(payload.namespace)}/files?path=${slashPrefix(payload.path)}`;
        await axios.delete(URL);
    }

    async function exportFileDirectory(this: any, payload: {namespace: string}) {
        const URL = `${base(payload.namespace)}/files/export`;
        const request = await axios.get(URL);

        const name = payload.namespace + "_files.zip";
        Utils.downloadUrl(request.request.responseURL, name);
    }

    return {
        autocomplete,
        loadAutocomplete,
        search,
        total,
        load,
        update,
        loadNamespaceSettings,
        updateNamespaceSettings,
        loadDependencies,
        existing,
        namespace,
        namespaces,
        namespaceSettings,
        inheritedVariables,
        setNamespaceVariables,
        inheritedPluginDefaults,
        setNamespacePluginDefaults,
        secrets,
        inheritedSecrets,
        kvs,
        inheritedKVModalVisible,
        addKvModalVisible,
        kvsList,
        kv,
        loadInheritedKVs,
        inheritedKVs,
        createKv,
        deleteKv,
        deleteKvs,
        loadInheritedSecrets,
        listSecrets,
        usableSecrets,
        createSecrets,
        patchSecret,
        deleteSecrets,
        loadInheritedVariables,
        loadInheritedPluginDefaults,
        createDirectory,
        readDirectory,
        saveOrCreateFile: createFile,
        readFile,
        fileRevisions,
        searchFiles,
        importFileDirectory,
        moveFileDirectory,
        renameFileDirectory,
        deleteFileDirectory,
        exportFileDirectory,
    };
}
