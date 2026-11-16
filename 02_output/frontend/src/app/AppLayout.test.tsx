import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createMemoryRouter, RouterProvider } from "react-router";
import { describe, expect, it } from "vitest";
import { routes } from "./routes";

function renderAt(path: string) {
  render(<RouterProvider router={createMemoryRouter(routes, { initialEntries: [path] })} />);
}

describe("base layout (UI-10, R-020-01)", () => {
  it("R-020-01 shows the Slovenian page title by default", () => {
    renderAt("/");
    expect(screen.getByRole("heading", { level: 1, name: "Kontakti" })).toBeTruthy();
    expect(screen.getByRole("banner")).toBeTruthy();
    expect(screen.getByRole("main")).toBeTruthy();
  });

  it("UI-23 the menu button toggles the navigation", async () => {
    renderAt("/");
    const button = screen.getByRole("button", { name: "Odpri meni" });
    expect(button.getAttribute("aria-expanded")).toBe("false");
    await userEvent.click(button);
    expect(button.getAttribute("aria-expanded")).toBe("true");
  });

  it("BR-02 an unknown route shows the not found page", () => {
    renderAt("/no-such-page");
    expect(screen.getByRole("heading", { level: 1, name: "Ni najdeno" })).toBeTruthy();
  });
});
