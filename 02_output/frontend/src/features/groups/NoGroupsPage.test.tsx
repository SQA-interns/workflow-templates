import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createMemoryRouter, RouterProvider } from "react-router";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import i18n from "../../i18n";
import { NoGroupsPage } from "./NoGroupsPage";

beforeEach(async () => {
  await i18n.changeLanguage("en");
});
afterEach(async () => {
  await i18n.changeLanguage("sl");
});

describe("WF-05 No Groups yet (R-001-07)", () => {
  it("AC-001-01 You have no Groups yet. with Create a Group", async () => {
    const router = createMemoryRouter(
      [
        { path: "/", element: <NoGroupsPage /> },
        { path: "/groups/new", element: <p>create group page</p> },
      ],
      { initialEntries: ["/"] },
    );
    render(<RouterProvider router={router} />);

    expect(screen.getByRole("heading", { level: 1, name: "You have no Groups yet." })).toBeTruthy();
    expect(screen.getByText("A Group keeps the Contacts of one project or company.")).toBeTruthy();
    await userEvent.click(screen.getByRole("button", { name: "Create a Group" }));

    expect(router.state.location.pathname).toBe("/groups/new");
  });
});
