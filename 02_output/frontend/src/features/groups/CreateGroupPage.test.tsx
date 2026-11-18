import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createMemoryRouter, RouterProvider } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import { ApiError } from "../../api/problem";
import i18n from "../../i18n";
import { CreateGroupPage } from "./CreateGroupPage";

vi.mock("../../api/apiClient", () => ({ apiRequest: vi.fn() }));

function renderPage() {
  const router = createMemoryRouter(
    [
      { path: "/", element: <p>start</p> },
      { path: "/groups/new", element: <CreateGroupPage /> },
      { path: "/groups/:groupId/contacts", element: <p>contact list</p> },
    ],
    { initialEntries: ["/", "/groups/new"], initialIndex: 1 },
  );
  render(
    <QueryClientProvider client={new QueryClient()}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  );
  return router;
}

async function submit(name: string) {
  if (name) await userEvent.type(screen.getByLabelText(/Group name/), name);
  await userEvent.click(screen.getByRole("button", { name: "Create a Group" }));
}

beforeEach(async () => {
  vi.mocked(apiRequest).mockReset();
  vi.mocked(apiRequest).mockResolvedValue({ id: "g1", name: "Konferenca 2025", role: "OWNER" });
  await i18n.changeLanguage("en");
});
afterEach(async () => {
  await i18n.changeLanguage("sl");
});

describe("WF-04 Create a Group (R-002-01, R-002-02)", () => {
  it("AC-002-01 the new Group opens as the current Group", async () => {
    const router = renderPage();
    expect(screen.getByRole("heading", { level: 1, name: "Create a Group" })).toBeTruthy();

    await submit("  Konferenca 2025  ");

    expect(apiRequest).toHaveBeenCalledWith("POST", "/groups", { name: "Konferenca 2025" });
    await waitFor(() => expect(router.state.location.pathname).toBe("/groups/g1/contacts"));
  });

  it("AC-002-04 a blank name: Enter a name of 1 to 100 characters. and nothing is sent", async () => {
    renderPage();

    await submit("   ");

    expect((await screen.findByRole("alert")).textContent).toBe(
      "Enter a name of 1 to 100 characters.",
    );
    expect(screen.getByLabelText(/Group name/).getAttribute("aria-invalid")).toBe("true");
    expect(apiRequest).not.toHaveBeenCalled();
  });

  it("EX-002-02 a name of 101 characters is rejected before sending", async () => {
    renderPage();

    await submit("K".repeat(101));

    expect((await screen.findByRole("alert")).textContent).toBe(
      "Enter a name of 1 to 100 characters.",
    );
    expect(apiRequest).not.toHaveBeenCalled();
  });

  it("R-002-02 the server's group.nameLength shows the same text", async () => {
    vi.mocked(apiRequest).mockRejectedValue(
      new ApiError(400, {
        code: "validation",
        errors: [{ field: "name", code: "group.nameLength" }],
      }),
    );
    renderPage();

    await submit("Konferenca 2025");

    expect((await screen.findByRole("alert")).textContent).toBe(
      "Enter a name of 1 to 100 characters.",
    );
  });

  it("UI-14 any other failure shows the error state and keeps the form", async () => {
    vi.mocked(apiRequest).mockRejectedValue(new ApiError(500, { code: "internal" }));
    renderPage();

    await submit("Konferenca 2025");

    expect((await screen.findByRole("alert")).textContent).toBe("Something went wrong.");
    expect((screen.getByLabelText(/Group name/) as HTMLInputElement).value).toBe("Konferenca 2025");
  });

  it("UI-12 Cancel goes back without creating a Group", async () => {
    const router = renderPage();

    await userEvent.click(screen.getByRole("button", { name: "Cancel" }));

    expect(router.state.location.pathname).toBe("/");
    expect(apiRequest).not.toHaveBeenCalled();
  });
  it("R-002-02 the name field is described by its rule and valid until a check fails", async () => {
    renderPage();

    const field = screen.getByLabelText(/Group name/);
    expect(field.getAttribute("aria-invalid")).toBe("false");
    const hint = document.getElementById(field.getAttribute("aria-describedby") ?? "");
    expect(hint?.textContent).toBe("1 to 100 characters, e.g. a project or a company.");
  });
});
