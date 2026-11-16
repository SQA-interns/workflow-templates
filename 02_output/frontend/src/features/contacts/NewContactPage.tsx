import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate, useParams } from "react-router";
import { useCreateContact, useEmailMatches } from "../../api/contacts";
import { isEmailAddress } from "./contactFields";
import { ContactForm } from "./ContactForm";

/**
 * WF-08 Add a Contact (US-006, US-008, US-009): saving opens the Group's Contact list, where it is listed.
 */
export function NewContactPage() {
  const { t } = useTranslation();
  const { groupId = "" } = useParams();
  const navigate = useNavigate();
  const createContact = useCreateContact(groupId);
  const [lookedUp, setLookedUp] = useState<string | null>(null);
  const matches = useEmailMatches(groupId, lookedUp).data ?? [];
  const contactList = `/groups/${groupId}/contacts`;

  /** R-006-06: a valid e-mail is looked up when the User leaves its box. */
  function lookUp(email: string) {
    const trimmed = email.trim();
    setLookedUp(isEmailAddress(trimmed) ? trimmed : null);
  }

  return (
    <ContactForm
      title={t("contacts.new.title")}
      backTo={contactList}
      busy={createContact.isPending}
      error={createContact.error}
      onSave={(fields, attributes, tags) =>
        createContact.mutate(
          { fields, attributes, tags },
          { onSuccess: () => void navigate(contactList) },
        )
      }
      onCancel={() => void navigate(contactList)}
      onEmailLeave={lookUp}
      emailNotice={
        lookedUp && matches.length > 0
          ? t("contacts.emailExists", { name: matches.map((match) => match.name).join(", ") })
          : ""
      }
    />
  );
}
