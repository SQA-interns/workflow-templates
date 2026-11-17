import type { AttributeRow } from "../features/contacts/attributes";
import type { ContactFields } from "../features/contacts/contactFields";

/** One changed item of a Version (`Change` of the contract). */
export interface Change {
  item: string;
  oldValue: string | null;
  newValue: string | null;
}

/** One entry of the History (`VersionSummary` of the contract, R-010-01). */
export interface VersionSummary {
  number: number;
  kind: string;
  changedBy: string;
  changedAt: string;
  changes: Change[];
}

/** One Version with the content after it (`Version` of the contract, R-010-02). */
export interface Version extends VersionSummary {
  snapshot: { fields: ContactFields; attributes: AttributeRow[]; tags: string[] };
}
