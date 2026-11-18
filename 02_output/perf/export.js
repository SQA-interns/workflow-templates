// NFR-02 (and NFR-03 with perf/seed-capacity.sql), specification §8: one timed Export of the
// 10,000 Contacts of "Konferenca 2025". Pass value: duration < 10 s and a file of 10,000 rows.
// The Export scope is the list query (BR-09): the search "priimek" finds exactly the 10,000 Contacts
// of perf/seed-10k.sql (not the three of seed/seed.sql), which the script checks first; k6 cannot
// unzip the .xlsx, so perf/README.md counts the file's rows with unzip.
import { check } from "k6";
import { api, groupIdOf, signIn } from "./support.js";

const SCOPE = { q: "priimek" };

export const options = {
  scenarios: {
    export: {
      executor: "shared-iterations",
      vus: 1,
      iterations: 1,
      maxDuration: "1m",
    },
  },
  thresholds: {
    "http_req_duration{scenario:export}": ["max<10000"],
    "checks{scenario:export}": ["rate==1"],
  },
};

export function setup() {
  const ana = signIn("ana.kovac@example.si", "Ana Kovač");
  const groupId = groupIdOf(ana);
  const total = api(
    "GET",
    `/groups/${groupId}/contacts?q=${SCOPE.q}`,
    ana,
  ).json().total;
  if (total !== 10000) {
    throw new Error(
      `the search finds ${total} Contacts, not 10,000: load perf/seed-10k.sql`,
    );
  }
  return { ana, groupId };
}

export default function (data) {
  const res = api("POST", `/groups/${data.groupId}/exports`, data.ana, SCOPE, {
    responseType: "binary",
    timeout: "60s",
  });
  check(res, {
    "export answers 200": (r) => r.status === 200,
    "export is an .xlsx": (r) =>
      String(r.headers["Content-Type"]).startsWith(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      ),
  });
  console.log(
    `export: ${res.status}, ${res.body.byteLength} bytes, ${res.timings.duration} ms`,
  );
}
