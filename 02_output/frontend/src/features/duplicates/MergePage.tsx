import { ArrowLeft } from "lucide-react";
import { type ReactNode, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate, useParams, useSearchParams } from "react-router";
import { Button } from "@/components/ui/button";
import { type Contact, useContact } from "../../api/contacts";
import { type Choice, useMerge } from "../../api/duplicates";
import { ApiError, messageKey } from "../../api/problem";
import { Section } from "../contacts/ContactContent";
import { contactName } from "../contacts/contactFields";
import { GroupsPending } from "../groups/GroupsPending";
import { fieldValue, mergedAttributes, mergedTags, preselected, shownFields } from "./mergeChoices";

/**
 * WF-18 Merge (US-018): the pair of `?first=&second=` side by side, the first as Primary contact,
 * a value per Field (R-018-01), a choice for every Attribute key with two values (R-018-03), the
 * Tags of both; Merge sends it (R-018-02), Cancel changes nothing (EX-018-06).
 */
export function MergePage() {
  const { groupId = "" } = useParams();
  const [search] = useSearchParams();
  const first = useContact(groupId, search.get("first") ?? "");
  const second = useContact(groupId, search.get("second") ?? "");

  if (!first.isSuccess) return <Page groupId={groupId}>{<GroupsPending query={first} />}</Page>;
  if (!second.isSuccess) return <Page groupId={groupId}>{<GroupsPending query={second} />}</Page>;
  return (
    <Page groupId={groupId}>
      <MergeForm groupId={groupId} first={first.data} second={second.data} />
    </Page>
  );
}

function Page({ groupId, children }: { groupId: string; children: ReactNode }) {
  const { t } = useTranslation();
  return (
    <div className="flex flex-col gap-4">
      <Link
        to={`/groups/${groupId}/duplicates`}
        className="inline-flex min-h-11 items-center gap-1 self-start underline"
      >
        <ArrowLeft aria-hidden="true" className="size-4" />
        {t("nav.duplicates")}
      </Link>
      <h1 className="text-2xl font-semibold">{t("merge.title")}</h1>
      <p className="text-text-muted">{t("merge.intro")}</p>
      {children}
    </div>
  );
}

function MergeForm({
  groupId,
  first,
  second,
}: {
  groupId: string;
  first: Contact;
  second: Contact;
}) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const merge = useMerge(groupId);
  const [primary, setPrimary] = useState(first);
  const other = primary.id === first.id ? second : first;
  const [fieldChoices, setFieldChoices] = useState(() => preselected(first, second));
  const [attributeChoices, setAttributeChoices] = useState<Record<string, Choice>>({});
  const error = merge.error instanceof ApiError ? merge.error : null;

  function choosePrimary(contact: Contact) {
    setPrimary(contact);
    setFieldChoices(preselected(contact, contact.id === first.id ? second : first));
    setAttributeChoices({});
  }

  function confirm() {
    merge.mutate(
      {
        primaryId: primary.id,
        otherId: other.id,
        primaryVersionNumber: primary.versionNumber,
        otherVersionNumber: other.versionNumber,
        fieldChoices,
        attributeChoices,
      },
      { onSuccess: () => void navigate(`/groups/${groupId}/contacts/${primary.id}`) },
    );
  }

  return (
    <>
      <Section title={t("merge.primary")}>
        <div className="flex flex-wrap gap-x-6">
          {[first, second].map((contact) => (
            <Option
              key={contact.id}
              group="primary"
              label={contactName(contact.fields)}
              checked={contact.id === primary.id}
              onChoose={() => choosePrimary(contact)}
            />
          ))}
        </div>
      </Section>
      <FieldChoices
        primary={primary}
        other={other}
        choices={fieldChoices}
        onChoose={(name, choice) => setFieldChoices({ ...fieldChoices, [name]: choice })}
      />
      <AttributeChoices
        primary={primary}
        other={other}
        choices={attributeChoices}
        onChoose={(key, choice) => setAttributeChoices({ ...attributeChoices, [key]: choice })}
        problem={error?.code === "merge.choiceRequired" ? error : null}
      />
      <Section title={t("contacts.details.tags")}>
        <p className="flex flex-wrap gap-1">
          {mergedTags(primary, other).map((name) => (
            <span key={name} className="rounded-full border border-border px-2 py-0.5 text-sm">
              {name}
            </span>
          ))}
        </p>
      </Section>
      {merge.isError && error?.code !== "merge.choiceRequired" && (
        <p role="alert" className="border-l-4 border-text pl-2 font-semibold">
          {error ? t(messageKey(error.code), error.params) : t("state.error")}
        </p>
      )}
      <div className="flex flex-wrap gap-2">
        <Button onClick={confirm} disabled={merge.isPending}>
          {t("merge.confirm")}
        </Button>
        <Button variant="ghost" onClick={() => void navigate(`/groups/${groupId}/duplicates`)}>
          {t("merge.cancel")}
        </Button>
      </div>
    </>
  );
}

interface ChoicesProps {
  primary: Contact;
  other: Contact;
  choices: Record<string, Choice>;
  onChoose: (item: string, choice: Choice) => void;
}

/** One row per shown Field with a radio for each Contact's value (R-018-01, R-018-02). */
function FieldChoices({ primary, other, choices, onChoose }: ChoicesProps) {
  const { t } = useTranslation();
  const chosen = new Map(Object.entries(choices));
  const head = "border-b border-border p-2 text-left font-semibold";
  return (
    <Section title={t("contacts.fields")}>
      <table className="w-full table-fixed border-collapse">
        <thead>
          <tr>
            <th scope="col" className={head}>
              {t("merge.field")}
            </th>
            <th scope="col" className={head}>
              {t("merge.primary")}
            </th>
            <th scope="col" className={head}>
              {t("merge.other")}
            </th>
          </tr>
        </thead>
        <tbody>
          {shownFields(primary, other).map((name) => (
            <tr key={name} className="border-b border-border">
              <th scope="row" className="p-2 text-left font-normal break-words">
                {t(`contacts.field.${name}`)}
              </th>
              {(["primary", "other"] as const).map((choice) => (
                <td key={choice} className="p-2 break-words">
                  <Option
                    group={`field-${name}`}
                    label={
                      fieldValue(choice === "primary" ? primary : other, name) ?? t("merge.empty")
                    }
                    checked={chosen.get(name) === choice}
                    onChoose={() => onChoose(name, choice)}
                  />
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </Section>
  );
}

/** The Attributes of both; a key with two values gets a choice (R-018-03, EX-018-01). */
function AttributeChoices({
  primary,
  other,
  choices,
  onChoose,
  problem,
}: ChoicesProps & { problem: ApiError | null }) {
  const { t } = useTranslation();
  const chosen = new Map(Object.entries(choices));
  return (
    <Section title={t("contacts.details.attributes")}>
      {mergedAttributes(primary.attributes, other.attributes).map((attribute) =>
        attribute.other === undefined ? (
          <p key={attribute.key} className="break-words">
            <span className="text-text-muted">{attribute.key}</span> {attribute.value}
          </p>
        ) : (
          <fieldset key={attribute.key} className="flex flex-wrap gap-x-6">
            <legend className="text-text-muted">{attribute.key}</legend>
            {(["primary", "other"] as const).map((choice) => (
              <Option
                key={choice}
                group={`attribute-${attribute.key}`}
                label={(choice === "primary" ? attribute.value : attribute.other) ?? ""}
                checked={chosen.get(attribute.key) === choice}
                onChoose={() => onChoose(attribute.key, choice)}
              />
            ))}
          </fieldset>
        ),
      )}
      {problem && (
        <p role="alert" className="border-l-4 border-text pl-2 font-semibold">
          {t(messageKey(problem.code), problem.params)}
        </p>
      )}
    </Section>
  );
}

/** A radio named by its label, 44 px high (UI-20). */
function Option(props: { group: string; label: string; checked: boolean; onChoose: () => void }) {
  return (
    <label className="inline-flex min-h-11 items-center gap-2 break-words">
      <input
        type="radio"
        name={props.group}
        checked={props.checked}
        onChange={props.onChoose}
        className="size-5 shrink-0"
      />
      {props.label}
    </label>
  );
}
