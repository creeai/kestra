import {useRoute} from "vue-router";
import {useI18n} from "vue-i18n";

import {
    Tab,
    ORDER,
    useHelpers,
} from "../../../components/namespaces/utils/useHelpers";

import DemoNamespace from "../../../components/demo/Namespace.vue";
import NamespaceAccess from "./NamespaceAccess.vue";
import NamespaceEdit from "./NamespaceEdit.vue";
import NamespaceSecrets from "./NamespaceSecrets.vue";
import NamespaceVariables from "./NamespaceVariables.vue";
import NamespacePluginDefaults from "./NamespacePluginDefaults.vue";
import KVTable from "../../../components/kv/KVTable.vue";

const lockedProps = (tab: string) => ({
    locked: true,
    component: DemoNamespace,
    props: {tab},
});

export function useTabs() {
    const route = useRoute();
    const {t} = useI18n({useScope: "global"});

    const namespace = route.params?.id as string;

    const tabs: Tab[] = [
        ...useHelpers().tabs,
        {
            name: "access",
            title: t("namespaceForm.access"),
            component: NamespaceAccess,
            props: {namespace},
        },
        {
            name: "edit",
            title: t("edit"),
            component: NamespaceEdit,
            props: {namespace},
        },
        {
            name: "secrets",
            title: t("secret.names"),
            component: NamespaceSecrets,
            props: {namespace},
        },
        {
            ...lockedProps("assets"),
            name: "assets",
            title: t("assets.title"),
        },
        {
            name: "variables",
            title: t("variables"),
            component: NamespaceVariables,
            props: {namespace},
        },
        {
            name: "plugin-defaults",
            title: t("pluginDefaults.title"),
            component: NamespacePluginDefaults,
            props: {namespace},
        },
        {
            name: "kv",
            title: t("kv.name"),
            component: KVTable,
            props: {namespace},
        },
        {
            ...lockedProps("history"),
            name: "history",
            title: t("revisions"),
        },
        {
            ...lockedProps("audit-logs"),
            name: "audit-logs",
            title: t("auditlogs"),
        },
    ];

    // Ensure the order of tabs (include "access" after overview)
    const orderWithAccess = [...ORDER.slice(0, ORDER.indexOf("overview") + 1), "access", ...ORDER.slice(ORDER.indexOf("overview") + 1)];
    tabs.sort((a, b) => orderWithAccess.indexOf(a.name) - orderWithAccess.indexOf(b.name));

    return {tabs};
}
