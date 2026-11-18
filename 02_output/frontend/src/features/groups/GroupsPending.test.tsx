import type { UseQueryResult } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import i18n from "../../i18n";
import { GroupsPending } from "./GroupsPending";

function query(state: Partial<UseQueryResult>): UseQueryResult {
  return { isPending: false, isError: false, refetch: vi.fn(), ...state } as UseQueryResult;
}

beforeEach(async () => {
  await i18n.changeLanguage("en");
});
afterEach(async () => {
  await i18n.changeLanguage("sl");
});

describe("GroupsPending (UI-14)", () => {
  it("UI-14 shows Loading … while the query is pending", () => {
    render(<GroupsPending query={query({ isPending: true })} />);

    expect(screen.getByRole("status").textContent).toBe("Loading …");
  });

  it("UI-14 shows the error with Try again, which asks again", async () => {
    const failed = query({ isError: true });
    render(<GroupsPending query={failed} />);

    await userEvent.click(screen.getByRole("button", { name: "Try again" }));

    expect(screen.getByRole("alert").textContent).toContain("Something went wrong.");
    expect(failed.refetch).toHaveBeenCalled();
  });

  it("UI-14 shows nothing once the data is there", () => {
    const { container } = render(<GroupsPending query={query({ isSuccess: true })} />);

    expect(container.textContent).toBe("");
    expect(screen.queryByRole("alert")).toBeNull();
  });
});
