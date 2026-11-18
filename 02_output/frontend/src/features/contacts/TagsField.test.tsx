import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { createMemoryRouter, MemoryRouter, Route, Routes, RouterProvider } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import { ApiError } from "../../api/problem";
import i18n from "../../i18n";
import { emptyFields } from "./contactFields";
import { EditContactPage } from "./EditContactPage";
import { NewContactPage } from "./NewContactPage";
import { TagsField } from "./TagsField";

vi.mock("../../api/apiClient", () => ({ apiRequest: vi.fn() }));

const DETAILS = "/groups/g1/contacts/c1";
const MESSAGE = "A Tag must have 1 to 50 characters and no comma.";

function janez(...tags: string[]) {
  return {
    id: "c1",
    versionNumber: 1,
    state: "active",
    fields: { ...emptyFields(), firstName: "Janez", lastName: "Novak" },
    attributes: [],
    tags: tags.map((name, i) => ({ id: `t${i}`, name })),
    createdAt: "2026-03-05T09:00:00Z",
    updatedAt: "2026-03-05T10:00:00Z",
    mergedFrom: [],
    mergedInto: null,
  };
}

/** Answers the Contact, the suggested Tags and saves with the save function. */
function answer(
  contact: unknown,
  suggested: string[] = [],
  save: () => Promise<unknown> = () => Promise.resolve({ id: "c1" }),
) {
  vi.mocked(apiRequest).mockImplementation((method: string, path: string) => {
    if (method !== "GET") return save();
    if (path.includes("/tags?q=")) {
      return Promise.resolve(suggested.map((name, i) => ({ id: `s${i}`, name })));
    }
    return Promise.resolve(contact);
  });
}

function renderAt(path: string) {
  const router = createMemoryRouter(
    [
      { path: "/groups/:groupId/contacts", element: <p>contact list</p> },
      { path: "/groups/:groupId/contacts/new", element: <NewContactPage /> },
      { path: "/groups/:groupId/contacts/:contactId", element: <p>contact details</p> },
      { path: "/groups/:groupId/contacts/:contactId/edit", element: <EditContactPage /> },
    ],
    { initialEntries: [path] },
  );
  render(
    <QueryClientProvider
      client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
    >
      <RouterProvider router={router} />
    </QueryClientProvider>,
  );
  return router;
}

async function editJanez() {
  const router = renderAt(`${DETAILS}/edit`);
  await screen.findByDisplayValue("Janez");
  return router;
}

async function addTag(name: string) {
  await userEvent.type(screen.getByLabelText("Add a Tag"), `${name}{Enter}`);
}

/** Puts the text into the Tag box in one change, as the suggestions need only the final text. */
async function typeInTagBox(text: string) {
  await userEvent.click(screen.getByRole("combobox", { name: "Add a Tag" }));
  await userEvent.paste(text);
}

async function save() {
  await userEvent.click(screen.getByRole("button", { name: "Save" }));
}

function sentTags(method: "PUT" | "POST") {
  return vi
    .mocked(apiRequest)
    .mock.calls.filter(([m]) => m === method)
    .map(([, , body]) => (body as { tags: unknown }).tags);
}

/** A suggested Tag; it is asked 300 ms after the last key, so a busy run may need longer. */
function suggestion(name: string) {
  return screen.findByRole("option", { name }, { timeout: 3000 });
}

function tagsSection() {
  return screen.getByRole("heading", { level: 2, name: "Tags" }).closest("section") as HTMLElement;
}

beforeEach(async () => {
  vi.mocked(apiRequest).mockReset();
  await i18n.changeLanguage("en");
});
afterEach(async () => {
  await i18n.changeLanguage("sl");
});

describe("WF-08 Tags of a Contact (US-009)", () => {
  it("R-009-01 the form shows the Contact's Tags with the hint", async () => {
    answer(janez("neObvescaj", "udelezenec2025"));
    await editJanez();

    expect(within(tagsSection()).getByRole("button", { name: "Remove neObvescaj" })).toBeTruthy();
    expect(screen.getByText("Press Enter to add. 1 to 50 characters, no comma.")).toBeTruthy();
  });

  it("AC-009-01 an added Tag is shown and sent with the Fields", async () => {
    answer(janez());
    const router = await editJanez();

    await addTag(" udelezenec2025 ");
    expect(within(tagsSection()).getByText("udelezenec2025")).toBeTruthy();
    await save();

    await waitFor(() => expect(router.state.location.pathname).toBe(DETAILS));
    expect(sentTags("PUT")).toEqual([["udelezenec2025"]]);
  });

  it("AC-009-04 removing a Tag takes it out of the save", async () => {
    answer(janez("neObvescaj", "udelezenec2025"));
    await editJanez();

    await userEvent.click(screen.getByRole("button", { name: "Remove neObvescaj" }));
    await save();

    await waitFor(() => expect(sentTags("PUT")).toEqual([["udelezenec2025"]]));
  });

  it("AC-009-02 typing part of a Tag suggests the Group's Tag", async () => {
    answer(janez(), ["udelezenec2025"]);
    await editJanez();

    await typeInTagBox("udel");

    expect(await suggestion("udelezenec2025")).toBeTruthy();
    expect(vi.mocked(apiRequest)).toHaveBeenCalledWith("GET", "/groups/g1/tags?q=udel");
  });

  it("R-009-02 choosing a suggestion adds it", async () => {
    answer(janez(), ["udelezenec2025"]);
    await editJanez();

    await typeInTagBox("udel");
    await userEvent.click(await suggestion("udelezenec2025"));
    expect(screen.getByLabelText<HTMLInputElement>("Add a Tag").value).toBe("");
    await save();

    await waitFor(() => expect(sentTags("PUT")).toEqual([["udelezenec2025"]]));
  });

  it("R-009-02 the arrow keys choose a suggestion and Enter adds it", async () => {
    answer(janez(), ["udelezenec2025", "udeležba"]);
    await editJanez();

    await typeInTagBox("ude");
    await suggestion("udeležba");
    await userEvent.keyboard("{ArrowDown}{ArrowDown}{Enter}");

    expect(within(tagsSection()).getByRole("button", { name: "Remove udeležba" })).toBeTruthy();
  });

  it("R-009-02 a Tag the Contact has is not suggested", async () => {
    answer(janez("udelezenec2025"), ["udelezenec2025", "udeležba"]);
    await editJanez();

    await typeInTagBox("ude");

    await suggestion("udeležba");
    expect(screen.queryByRole("option", { name: "udelezenec2025" })).toBeNull();
  });

  it("AC-009-03 a name in other case takes the spelling of the suggested Tag", async () => {
    answer(janez(), ["udelezenec2025"]);
    await editJanez();

    await typeInTagBox("Udelezenec2025");
    await suggestion("udelezenec2025");
    await userEvent.keyboard("{Enter}");

    expect(
      within(tagsSection()).getByRole("button", { name: "Remove udelezenec2025" }),
    ).toBeTruthy();
  });

  it("EX-009-03 a Tag the Contact has is not added twice", async () => {
    answer(janez("vip"));
    await editJanez();

    await addTag("VIP");

    expect(within(tagsSection()).getAllByRole("button", { name: /Remove/ })).toHaveLength(1);
  });

  it("AC-009-05 a name with a comma shows the message at once and adds nothing", async () => {
    answer(janez());
    await editJanez();

    await addTag("vip, 2025");

    expect(screen.getByText(MESSAGE)).toBeTruthy();
    expect(screen.getByLabelText("Add a Tag").getAttribute("aria-invalid")).toBe("true");
    expect(within(tagsSection()).queryAllByRole("button", { name: /Remove/ })).toHaveLength(0);
  });

  it("R-009-05 an invalid name left in the box stops the save", async () => {
    answer(janez());
    await editJanez();

    await userEvent.type(screen.getByLabelText("Add a Tag"), "t".repeat(51));
    await save();

    expect(await screen.findByText(MESSAGE)).toBeTruthy();
    expect(screen.getByLabelText("Add a Tag")).toBe(document.activeElement);
    expect(sentTags("PUT")).toEqual([]);
  });

  it("R-009-01 a valid name left in the box is saved with the Tags", async () => {
    answer(janez("vip"));
    await editJanez();

    await userEvent.type(screen.getByLabelText("Add a Tag"), "udeležba");
    await save();

    await waitFor(() => expect(sentTags("PUT")).toEqual([["vip", "udeležba"]]));
  });

  it("R-009-05 the API's tag.invalid stands in the Tags section", async () => {
    answer(janez(), [], () =>
      Promise.reject(
        new ApiError(400, {
          code: "validation",
          errors: [{ field: "tags[0]", code: "tag.invalid" }],
        }),
      ),
    );
    await editJanez();

    await addTag("vip");
    await save();

    expect(await within(tagsSection()).findByText(MESSAGE)).toBeTruthy();
  });

  it("R-009-01 a new Contact is added with its Tags", async () => {
    answer(null);
    renderAt("/groups/g1/contacts/new");

    await userEvent.type(await screen.findByLabelText("First name"), "Janez");
    await addTag("udelezenec2025");
    await save();

    await waitFor(() => expect(sentTags("POST")).toEqual([["udelezenec2025"]]));
  });
});

describe("WF-08 Tag box on its own (US-009, ARIA combobox)", () => {
  const NAMES = ["alpha", "beta", "gamma"];

  function Harness(props: {
    tags: string[];
    onAdd: (name: string, suggested: string[]) => void;
    error?: string;
  }) {
    const [text, setText] = useState("");
    return (
      <TagsField
        inputId="t"
        tags={props.tags}
        text={text}
        onTextChange={setText}
        onAdd={props.onAdd}
        onRemove={() => undefined}
        error={props.error}
      />
    );
  }

  function renderBox(error?: string) {
    const onAdd = vi.fn();
    const client = new QueryClient();
    const tree = (tags: string[]) => (
      <QueryClientProvider client={client}>
        <MemoryRouter initialEntries={["/groups/g1"]}>
          <Routes>
            <Route
              path="/groups/:groupId"
              element={<Harness tags={tags} onAdd={onAdd} error={error} />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );
    const { rerender } = render(tree([]));
    return { onAdd, setTags: (tags: string[]) => rerender(tree(tags)) };
  }

  function box() {
    return screen.getByRole("combobox", { name: "Add a Tag" });
  }

  /** Types "a" and waits for the three suggestions. */
  async function suggest() {
    await userEvent.type(box(), "a");
    await screen.findAllByRole("option", {}, { timeout: 3000 });
  }

  function activeOption() {
    const id = box().getAttribute("aria-activedescendant");
    return id === null ? null : document.getElementById(id);
  }

  function selected() {
    return screen
      .queryAllByRole("option")
      .filter((option) => option.getAttribute("aria-selected") === "true")
      .map((option) => option.textContent);
  }

  beforeEach(() => {
    answer(null, NAMES);
  });

  it("R-009-02 before suggestions the box is collapsed, controls the list and shows the hint", () => {
    renderBox();

    expect(box().getAttribute("aria-expanded")).toBe("false");
    const list = document.getElementById(box().getAttribute("aria-controls") ?? "");
    expect(list?.getAttribute("role")).toBe("listbox");
    expect(document.getElementById(box().getAttribute("aria-describedby") ?? "")?.textContent).toBe(
      "Press Enter to add. 1 to 50 characters, no comma.",
    );
    expect(screen.queryByRole("list")).toBeNull();
  });

  it("R-009-05 the Tags' problem marks the box and describes it before the hint", () => {
    renderBox(MESSAGE);

    expect(box().getAttribute("aria-invalid")).toBe("true");
    const [errorId, hintId] = (box().getAttribute("aria-describedby") ?? "").split(" ");
    expect(errorId).toBe(box().getAttribute("aria-errormessage"));
    expect(document.getElementById(errorId ?? "")?.textContent).toBe(MESSAGE);
    expect(document.getElementById(hintId ?? "")?.textContent).toBe(
      "Press Enter to add. 1 to 50 characters, no comma.",
    );
  });

  it("D-52 suggestions are asked once for the trimmed text after typing stops", async () => {
    renderBox();

    await userEvent.type(box(), " al");

    await waitFor(() =>
      expect(vi.mocked(apiRequest)).toHaveBeenCalledWith("GET", "/groups/g1/tags?q=al"),
    );
    expect(vi.mocked(apiRequest).mock.calls).toEqual([["GET", "/groups/g1/tags?q=al"]]);
  });

  it("R-009-02 ArrowUp walks the suggestions from the last and wraps; Escape leaves them", async () => {
    renderBox();
    await suggest();

    expect(box().getAttribute("aria-expanded")).toBe("true");
    expect(screen.getByRole("listbox", { name: "Suggested Tags" }).id).toBe(
      box().getAttribute("aria-controls"),
    );
    expect(box().getAttribute("aria-activedescendant")).toBeNull();
    expect(selected()).toEqual([]);
    const walk: (string | null | undefined)[] = [];
    for (let i = 0; i < 4; i += 1) {
      await userEvent.keyboard("{ArrowUp}");
      expect(selected()).toEqual([activeOption()?.textContent]);
      walk.push(activeOption()?.textContent);
    }
    expect(walk).toEqual(["gamma", "beta", "alpha", "gamma"]);

    await userEvent.keyboard("{Escape}");
    expect(box().getAttribute("aria-activedescendant")).toBeNull();
    expect(selected()).toEqual([]);
  });

  it("R-009-02 Enter adds the first suggestion when it is chosen", async () => {
    const { onAdd } = renderBox();
    await suggest();

    await userEvent.keyboard("{ArrowDown}{Enter}");

    expect(onAdd).toHaveBeenCalledWith("alpha", NAMES);
  });

  it("R-009-04 Enter without a chosen suggestion adds the typed text and chooses none", async () => {
    const { onAdd } = renderBox();
    await suggest();

    await userEvent.keyboard("{Enter}");

    expect(onAdd).toHaveBeenCalledWith("a", NAMES);
    expect(box().getAttribute("aria-activedescendant")).toBeNull();
  });

  it("R-009-02 typing again leaves the chosen suggestion", async () => {
    renderBox();
    await suggest();

    await userEvent.keyboard("{ArrowDown}l");

    expect(screen.getAllByRole("option")).toHaveLength(3);
    expect(box().getAttribute("aria-activedescendant")).toBeNull();
  });

  it("R-009-02 an emptied or blank box shows no suggestions at once", async () => {
    renderBox();
    await suggest();

    await userEvent.clear(box());
    expect(screen.queryAllByRole("option")).toEqual([]);
    await userEvent.type(box(), "  ");
    expect(screen.queryAllByRole("option")).toEqual([]);
  });

  it("R-009-02 clicking a suggestion adds it and keeps the focus in the box", async () => {
    const { onAdd } = renderBox();
    await suggest();

    await userEvent.click(screen.getByRole("option", { name: "beta" }));

    expect(onAdd).toHaveBeenCalledWith("beta", NAMES);
    expect(document.activeElement).toBe(box());
  });

  it("R-009-02 a chosen suggestion that is no longer offered is not added", async () => {
    const { onAdd, setTags } = renderBox();
    await suggest();
    await userEvent.keyboard("{ArrowUp}");

    setTags(NAMES);
    expect(box().getAttribute("aria-expanded")).toBe("false");
    expect(box().getAttribute("aria-activedescendant")).toBeNull();

    setTags(["gamma"]);
    await userEvent.keyboard("{Enter}");
    expect(onAdd).toHaveBeenCalledWith("a", NAMES);
  });
});
