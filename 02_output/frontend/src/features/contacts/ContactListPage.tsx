import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate, useParams, useSearchParams } from "react-router";
import { Button } from "@/components/ui/button";
import { useContactList } from "../../api/contacts";
import { useCanChangeContacts } from "../../api/groups";
import type { Language } from "../../i18n";
import { formatNumber } from "../../i18n/format";
import { ContactListControls } from "./ContactListControls";
import { ContactTable } from "./ContactTable";
import { type ListQuery, listQueryOf, searchParamsOf } from "./listQuery";

/** Wait after the last key before searching, so typing does not ask for every letter. */
const SEARCH_DELAY_MS = 300;

/** WF-06 Contact list of the current Group (US-014): count, search, sort, Pages, four states. */
export function ContactListPage() {
  const { t, i18n } = useTranslation();
  const { groupId = "" } = useParams();
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  const query = listQueryOf(params);
  const list = useContactList(groupId, query);
  const canAdd = useCanChangeContacts(groupId);
  const [search, setSearch] = useState(query.q);
  const [shownQ, setShownQ] = useState(query.q);
  if (query.q !== shownQ) {
    // The URL changed the search (Clear search, back button): the box follows it.
    setShownQ(query.q);
    setSearch(query.q);
  }

  function change(next: Partial<ListQuery>) {
    setParams(searchParamsOf({ ...query, page: 1, ...next }));
  }

  useEffect(() => {
    if (search === query.q) return;
    const timer = setTimeout(() => change({ q: search }), SEARCH_DELAY_MS);
    return () => clearTimeout(timer);
  });

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
      <ContactListControls
        search={search}
        sort={query.sort}
        dir={query.dir}
        onSearch={setSearch}
        onSort={(sort, dir) => change({ sort, dir })}
      />
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
