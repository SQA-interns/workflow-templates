import { useTranslation } from "react-i18next";
import { useNavigate, useParams, useSearchParams } from "react-router";
import { Button } from "@/components/ui/button";
import { useContactList } from "../../api/contacts";
import { useGroups } from "../../api/groups";
import type { Language } from "../../i18n";
import { formatNumber } from "../../i18n/format";
import { ContactTable } from "./ContactTable";
import { type ListQuery, listQueryOf, searchParamsOf } from "./listQuery";

/** WF-06 Contact list of the current Group (US-014): count, Pages, four states. */
export function ContactListPage() {
  const { t, i18n } = useTranslation();
  const { groupId = "" } = useParams();
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  const query = listQueryOf(params);
  const list = useContactList(groupId, query);
  const role = useGroups().data?.find((group) => group.id === groupId)?.role;
  const canAdd = role === "OWNER" || role === "EDITOR";
  function change(next: Partial<ListQuery>) {
    setParams(searchParamsOf({ ...query, page: 1, ...next }));
  }

  const total = list.data?.total ?? 0;
  const addContact = () => void navigate(`/groups/${groupId}/contacts/new`);
  const add = canAdd && <Button onClick={addContact}>{t("contacts.list.add")}</Button>;

  return (
    <div className="flex flex-col gap-4 max-md:pb-20">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="text-2xl font-semibold">{t("page.contacts.title")}</h1>
        <div className="flex items-center gap-4">
          {list.data && (
            <p className="text-text-muted">
              {t("contacts.list.count", {
                count: total,
                formatted: formatNumber(total, i18n.language as Language),
              })}
            </p>
          )}
          {add && <span className="max-md:hidden">{add}</span>}
        </div>
      </div>
      <ListBody
        list={list}
        query={query}
        groupId={groupId}
        add={add}
        onClear={() => change({ q: "" })}
        onPage={(page) => change({ page })}
      />
      {add && (
        <div className="fixed inset-x-0 bottom-0 border-t border-border bg-background p-4 md:hidden [&_button]:w-full">
          {add}
        </div>
      )}
    </div>
  );
}

interface ListBodyProps {
  list: ReturnType<typeof useContactList>;
  query: ListQuery;
  groupId: string;
  add: React.ReactNode;
  onClear: () => void;
  onPage: (page: number) => void;
}

/** The rows with their Pages, or the loading, error, empty or no-match state (UI-14, R-014-05). */
function ListBody({ list, query, groupId, add, onClear, onPage }: ListBodyProps) {
  const { t } = useTranslation();
  if (list.isPending) {
    return (
      <div role="status" aria-label={t("state.loading")} className="flex flex-col gap-3">
        {[0, 1, 2].map((bar) => (
          <span key={bar} className="h-4 w-2/3 rounded bg-surface-raised" />
        ))}
      </div>
    );
  }
  if (list.isError) {
    return (
      <div role="alert" className="flex flex-col items-start gap-2">
        <p>{t("state.error")}</p>
        <Button variant="secondary" onClick={() => void list.refetch()}>
          {t("state.retry")}
        </Button>
      </div>
    );
  }
  const { items, total } = list.data;
  if (total === 0) {
    return (
      <div className="flex flex-col items-start gap-2">
        <p>{t(query.q ? "contacts.list.noMatch" : "contacts.list.empty")}</p>
        {query.q ? (
          <Button variant="secondary" onClick={onClear}>
            {t("contacts.list.clearSearch")}
          </Button>
        ) : (
          add
        )}
      </div>
    );
  }
  const pages = Math.ceil(total / list.data.pageSize);
  return (
    <>
      <ContactTable groupId={groupId} items={items} />
      {pages > 1 && (
        <nav aria-label={t("contacts.list.pages")} className="flex items-center justify-between">
          <Button
            variant="secondary"
            disabled={query.page <= 1}
            onClick={() => onPage(query.page - 1)}
          >
            {t("contacts.list.previous")}
          </Button>
          <p>{t("contacts.list.pageOf", { page: query.page, pages })}</p>
          <Button
            variant="secondary"
            disabled={query.page >= pages}
            onClick={() => onPage(query.page + 1)}
          >
            {t("contacts.list.next")}
          </Button>
        </nav>
      )}
    </>
  );
}
