import { useTranslation } from "react-i18next";
import { Link } from "react-router";
import { Badge } from "@/components/ui/badge";
import type { ContactListItem } from "../../api/contacts";
import { contactName } from "./contactFields";
import type { Selection } from "./useSelection";

const COLUMNS = ["name", "company", "email", "phone", "tags"] as const;

interface ContactTableProps {
  groupId: string;
  items: ContactListItem[];
  /** The Checked contacts (R-016-02). */
  selection: Selection;
}

/**
 * The rows of one Page (R-014-01): a table from 768 px, stacked cards below it (UI-16), one set
 * of elements for both. The name opens the details (R-014-06); a checkbox checks the Contact for
 * "Export checked" (R-016-02).
 */
export function ContactTable({ groupId, items, selection }: ContactTableProps) {
  const { t } = useTranslation();
  const ids = items.map((contact) => contact.id);
  const allChecked = ids.length > 0 && ids.every(selection.isChecked);
  return (
    <table className="w-full border-collapse text-left max-md:block">
      <thead className="max-md:sr-only">
        <tr className="border-b border-border">
          <th scope="col" className="w-11 px-3 py-2">
            <CheckBox
              label={t("contacts.list.checkPage")}
              checked={allChecked}
              onChange={(on) => selection.checkAll(ids, on)}
            />
          </th>
          {COLUMNS.map((column) => (
            <th key={column} scope="col" className="px-3 py-2 text-sm font-semibold">
              {t(`contacts.list.column.${column}`)}
            </th>
          ))}
        </tr>
      </thead>
      <tbody className="max-md:flex max-md:flex-col max-md:gap-3">
        {items.map((contact) => (
          <tr
            key={contact.id}
            className="border-b border-border max-md:block max-md:rounded-lg max-md:border max-md:p-3"
          >
            <td className="px-3 py-2 max-md:inline-block max-md:p-0 max-md:pr-2">
              <CheckBox
                label={t("contacts.list.check", { name: contactName(contact) })}
                checked={selection.isChecked(contact.id)}
                onChange={(on) => selection.check(contact.id, on)}
              />
            </td>
            <td className="px-3 py-2 max-md:inline-block max-md:p-0">
              <Link
                to={`/groups/${groupId}/contacts/${contact.id}`}
                className="inline-flex min-h-11 items-center font-semibold underline"
              >
                {contactName(contact)}
              </Link>
            </td>
            <td className="px-3 py-2 max-md:block max-md:p-0">{contact.company}</td>
            <td className="px-3 py-2 break-all max-md:block max-md:p-0">{contact.email}</td>
            <td className="px-3 py-2 max-md:block max-md:p-0">{contact.phone}</td>
            <td className="px-3 py-2 max-md:block max-md:p-0 max-md:pt-1">
              <span className="flex flex-wrap gap-1">
                {contact.tags.map((tag) => (
                  <Badge key={tag.id}>{tag.name}</Badge>
                ))}
              </span>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

interface CheckBoxProps {
  label: string;
  checked: boolean;
  onChange: (on: boolean) => void;
}

/** A checkbox with a 44 px target (UI-21), named by its label. */
function CheckBox({ label, checked, onChange }: CheckBoxProps) {
  return (
    <span className="inline-flex size-11 items-center justify-center">
      <input
        type="checkbox"
        aria-label={label}
        checked={checked}
        onChange={(event) => onChange(event.target.checked)}
        className="size-5 accent-primary"
      />
    </span>
  );
}
