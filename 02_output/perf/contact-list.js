// NFR-01 (and NFR-03 with perf/seed-capacity.sql), specification §8: 20 virtual Users for 2 minutes
// mix list, search and filter requests on "Konferenca 2025" with 10,000 Contacts.
// Pass value: http_req_duration p(95) < 500 ms. Usage: perf/README.md.
import { check, sleep } from "k6";
import { api, groupIdOf, signIn, viewers } from "./support.js";

const VIRTUAL_USERS = 20;

export const options = {
  scenarios: {
    contact_list: {
      executor: "constant-vus",
      vus: VIRTUAL_USERS,
      duration: "2m",
    },
  },
  thresholds: {
    "http_req_duration{scenario:contact_list}": ["p(95)<500"],
    "checks{scenario:contact_list}": ["rate==1"],
  },
};

/** Ana's Group, its perf Tags and one token per virtual User. */
export function setup() {
  const ana = signIn("ana.kovac@example.si", "Ana Kovač");
  const groupId = groupIdOf(ana);
  const tags = api("GET", `/groups/${groupId}/tags?q=perf`, ana).json();
  if (tags.length < 10) {
    throw new Error("perf Tags missing: load perf/seed-10k.sql first");
  }
  return {
    groupId,
    tagIds: tags.map((t) => t.id),
    tokens: viewers(VIRTUAL_USERS, groupId, ana),
  };
}

/** List (sorted, paged), search and filter queries of US-014 and US-015. */
function queries(tagIds) {
  const page = 1 + Math.floor(Math.random() * 200);
  const tag = tagIds[Math.floor(Math.random() * tagIds.length)];
  const other = tagIds[Math.floor(Math.random() * tagIds.length)];
  return [
    `page=${page}`,
    `sort=company&dir=desc&page=${page}`,
    `sort=updatedAt&page=${page}`,
    "q=priimek01",
    "q=maribor",
    `q=oseba${Math.floor(Math.random() * 10000)}`,
    `hasTagIds=${tag}`,
    `hasTagIds=${tag}&withoutTagIds=${other}&page=2`,
    "attributeKey=kompetenca&attributeValue=kompetenca3",
    `company=${encodeURIComponent("Podjetje 07")}&country=SI`,
    `q=celje&hasTagIds=${tag}&sort=email`,
  ];
}

export default function (data) {
  const token = data.tokens[(__VU - 1) % data.tokens.length];
  const all = queries(data.tagIds);
  const query = all[Math.floor(Math.random() * all.length)];
  const res = api(
    "GET",
    `/groups/${data.groupId}/contacts?${query}`,
    token,
    null,
    {
      tags: { name: "listContacts" },
    },
  );
  check(res, { "list answers 200": (r) => r.status === 200 });
  sleep(1);
}
