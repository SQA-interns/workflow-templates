import type { RouteObject } from "react-router";
import { GuestOnly, RequireAuth } from "../auth/RequireAuth";
import { SignInPage } from "../features/account/SignInPage";
import { SignUpPage } from "../features/account/SignUpPage";
import { VerifyEmailPage } from "../features/account/VerifyEmailPage";
import { CreateGroupPage } from "../features/groups/CreateGroupPage";
import { GroupRoute } from "../features/groups/GroupRoute";
import { GroupStart } from "../features/groups/GroupStart";
import { AppLayout } from "./AppLayout";
import { ContactListPlaceholder, NotFoundPage } from "./pages";
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
      { index: true, element: <GroupStart /> },
      { path: "/groups/new", element: <CreateGroupPage /> },
      {
        path: "/groups/:groupId",
        element: <GroupRoute />,
        children: [{ path: "contacts", element: <ContactListPlaceholder /> }],
      },
      { path: "*", element: <NotFoundPage /> },
    ],
  },
];
