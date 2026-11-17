import type { RouteObject } from "react-router";
import { GuestOnly, RequireAuth } from "../auth/RequireAuth";
import { SignInPage } from "../features/account/SignInPage";
import { SignUpPage } from "../features/account/SignUpPage";
import { VerifyEmailPage } from "../features/account/VerifyEmailPage";
import { ContactDetailsPage } from "../features/contacts/ContactDetailsPage";
import { ContactListPage } from "../features/contacts/ContactListPage";
import { EditContactPage } from "../features/contacts/EditContactPage";
import { NewContactPage } from "../features/contacts/NewContactPage";
import { DuplicatesPage } from "../features/duplicates/DuplicatesPage";
import { MergePage } from "../features/duplicates/MergePage";
import { CreateGroupPage } from "../features/groups/CreateGroupPage";
import { DeletedGroupsPage } from "../features/groups/DeletedGroupsPage";
import { GroupRoute } from "../features/groups/GroupRoute";
import { GroupSettingsPage } from "../features/groups/GroupSettingsPage";
import { GroupStart } from "../features/groups/GroupStart";
import { HistoryPage } from "../features/history/HistoryPage";
import { VersionPage } from "../features/history/VersionPage";
import { InvitationLayout } from "../features/invitations/InvitationLayout";
import { InvitationPage } from "../features/invitations/InvitationPage";
import { MembersPage } from "../features/members/MembersPage";
import { RecycleBinPage } from "../features/recycle-bin/RecycleBinPage";
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
    path: "/invitations/:token",
    element: <InvitationLayout />,
    children: [{ index: true, element: <InvitationPage /> }],
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
      { path: "/deleted-groups", element: <DeletedGroupsPage /> },
      {
        path: "/groups/:groupId",
        element: <GroupRoute />,
        children: [
          { path: "contacts", element: <ContactListPage /> },
          { path: "contacts/new", element: <NewContactPage /> },
          { path: "contacts/:contactId", element: <ContactDetailsPage /> },
          { path: "contacts/:contactId/edit", element: <EditContactPage /> },
          { path: "contacts/:contactId/history", element: <HistoryPage /> },
          { path: "contacts/:contactId/history/:number", element: <VersionPage /> },
          { path: "recycle-bin", element: <RecycleBinPage /> },
          { path: "duplicates", element: <DuplicatesPage /> },
          { path: "merge", element: <MergePage /> },
          { path: "members", element: <MembersPage /> },
          { path: "settings", element: <GroupSettingsPage /> },
        ],
      },
      { path: "*", element: <NotFoundPage /> },
    ],
  },
];
