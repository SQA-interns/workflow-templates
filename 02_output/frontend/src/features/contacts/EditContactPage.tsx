import { useTranslation } from "react-i18next";
import { useNavigate, useParams } from "react-router";
import { useContact, useUpdateContact } from "../../api/contacts";
import { ApiError } from "../../api/problem";
import { NotFoundPage } from "../../app/pages";
import { GroupsPending } from "../groups/GroupsPending";
import { ContactForm } from "./ContactForm";

/** WF-08 Edit Contact (US-007): saving opens the Contact's details with the saved Version. */
export function EditContactPage() {
  const { t } = useTranslation();
  const { groupId = "", contactId = "" } = useParams();
  const navigate = useNavigate();
  const contact = useContact(groupId, contactId);
  const updateContact = useUpdateContact(groupId, contactId);
  const details = `/groups/${groupId}/contacts/${contactId}`;

  if (contact.error instanceof ApiError && contact.error.status === 404) return <NotFoundPage />;
  if (!contact.isSuccess) return <GroupsPending query={contact} />;
  const read = contact.data;

  /** BR-06: the form starts again from the current Version. */
  function reload() {
    updateContact.reset();
    void contact.refetch();
  }

  return (
    <ContactForm
      key={read.versionNumber}
      title={t("contacts.edit.title")}
      initial={read.fields}
      backTo={details}
      busy={updateContact.isPending}
      error={updateContact.error}
      onSave={(fields) =>
        updateContact.mutate({ read, fields }, { onSuccess: () => void navigate(details) })
      }
      onCancel={() => void navigate(details)}
      onReload={reload}
    />
  );
}
