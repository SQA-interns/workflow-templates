import { useTranslation } from "react-i18next";
import { useNavigate, useParams } from "react-router";
import { useCreateContact } from "../../api/contacts";
import { ContactForm } from "./ContactForm";

/** WF-08 Add a Contact (US-006): saving opens the Group's Contact list, where it is listed. */
export function NewContactPage() {
  const { t } = useTranslation();
  const { groupId = "" } = useParams();
  const navigate = useNavigate();
  const createContact = useCreateContact(groupId);
  const contactList = `/groups/${groupId}/contacts`;

  return (
    <ContactForm
      title={t("contacts.new.title")}
      backTo={contactList}
      busy={createContact.isPending}
      error={createContact.error}
      onSave={(fields) =>
        createContact.mutate(fields, { onSuccess: () => void navigate(contactList) })
      }
      onCancel={() => void navigate(contactList)}
    />
  );
}
