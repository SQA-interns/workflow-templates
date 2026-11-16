import type { RouteObject } from "react-router";
import { GuestOnly, RequireAuth } from "../auth/RequireAuth";
import { SignInPage } from "../features/account/SignInPage";
import { SignUpPage } from "../features/account/SignUpPage";
import { VerifyEmailPage } from "../features/account/VerifyEmailPage";
import { NoGroupsPage } from "../features/groups/NoGroupsPage";
import { AppLayout } from "./AppLayout";
import { NotFoundPage } from "./pages";
import { PublicLayout } from "./PublicLayout";

/** Route table (specification §9.1, §9.2); each story adds its routes here. */
export const routes: RouteObject[] = [
  {
    element: <PublicLayout />,
    children: [
      {
        path: "/sign-in",
        element: (
          <GuestOnly>
            <SignInPage />
          </GuestOnly>
        ),
      },
      {
        path: "/sign-up",
        element: (
          <GuestOnly>
            <SignUpPage />
          </GuestOnly>
        ),
      },
      { path: "/verify-email", element: <VerifyEmailPage /> },
    ],
  },
  {
    element: (
      <RequireAuth>
        <AppLayout />
      </RequireAuth>
    ),
    children: [
      { index: true, element: <NoGroupsPage /> },
      { path: "*", element: <NotFoundPage /> },
    ],
  },
];
