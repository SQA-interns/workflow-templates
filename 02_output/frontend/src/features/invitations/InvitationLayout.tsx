import { AppLayout } from "../../app/AppLayout";
import { PublicLayout } from "../../app/PublicLayout";
import { useAuth } from "../../auth/AuthProvider";
import { RequireAuth } from "../../auth/RequireAuth";

/**
 * `/invitations/:token` is open to visitors who are not signed in (R-004-07): they get the public
 * layout; a signed-in User gets the product layout after the usual checks (R-001-05).
 */
export function InvitationLayout() {
  const { state } = useAuth();
  if (state.status === "signedOut") return <PublicLayout />;
  return (
    <RequireAuth>
      <AppLayout />
    </RequireAuth>
  );
}
