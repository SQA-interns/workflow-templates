import { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { Link, useLocation, useNavigate, useParams } from "react-router";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { useAcceptInvitation, useInvitation } from "../../api/invitations";
import { ApiError, messageKey } from "../../api/problem";
import { useAuth } from "../../auth/AuthProvider";
import { forgetReturnTo, rememberReturnTo } from "../../auth/returnTo";
import { formatDateTime } from "../../i18n/format";

/**
 * WF-16 Invitation: the signed-in invitee sees the Group, the Role and the expiry and accepts
 * (R-004-04); refusals show their message (R-004-05, R-004-06); a visitor who is not signed in is
 * asked to sign in first and comes back here afterwards (R-004-07).
 */
export function InvitationPage() {
  const { state } = useAuth();
  return (
    <div className="mx-auto flex w-full max-w-xl flex-col gap-4">
      {state.status === "signedOut" ? <SignInFirst /> : <Invitation />}
    </div>
  );
}

function SignInFirst() {
  const { t } = useTranslation();
  const { pathname } = useLocation();
  useEffect(() => rememberReturnTo(pathname), [pathname]);
  return (
    <Card className="flex flex-col gap-3">
      <p>{t("invitations.page.signedOut")}</p>
      <div className="flex flex-wrap gap-2">
        <Button asChild>
          <Link to="/sign-in">{t("invitations.page.signIn")}</Link>
        </Button>
        <Button asChild variant="secondary">
          <Link to="/sign-up">{t("invitations.page.signUp")}</Link>
        </Button>
      </div>
    </Card>
  );
}

function Invitation() {
  const { t } = useTranslation();
  const { token = "" } = useParams();
  const navigate = useNavigate();
  const invitation = useInvitation(token);
  const accept = useAcceptInvitation(token);
  useEffect(forgetReturnTo, []);

  if (invitation.isPending) {
    return (
      <Card>
        <p role="status">{t("state.loading")}</p>
      </Card>
    );
  }
  if (invitation.isError) return <Refused error={invitation.error} retry={invitation.refetch} />;
  const { groupName, role, expiresAt } = invitation.data;
  return (
    <Card className="flex flex-col items-start gap-3">
      <h1 className="text-2xl font-semibold">{t("invitations.page.title", { name: groupName })}</h1>
      <p>{t("invitations.page.role", { role: t(`roles.${role}`) })}</p>
      <p className="text-text-muted">
        {t("invitations.page.expires", { date: formatDateTime(expiresAt) })}
      </p>
      {accept.isError && <ErrorText error={accept.error} />}
      <Button
        disabled={accept.isPending}
        aria-busy={accept.isPending}
        onClick={() =>
          accept.mutate(undefined, {
            onSuccess: (group) => void navigate(`/groups/${group.id}/contacts`),
          })
        }
      >
        {t("invitations.page.accept")}
      </Button>
    </Card>
  );
}

/** "for another e-mail" or "no longer valid" (R-004-05, R-004-06); otherwise Try again (UI-14). */
function Refused({ error, retry }: { error: Error; retry: () => unknown }) {
  const { t } = useTranslation();
  const known = error instanceof ApiError && error.status < 500;
  return (
    <Card className="flex flex-col items-start gap-3">
      <ErrorText error={error} />
      {!known && (
        <Button variant="secondary" onClick={() => void retry()}>
          {t("state.retry")}
        </Button>
      )}
    </Card>
  );
}

function ErrorText({ error }: { error: Error }) {
  const { t } = useTranslation();
  return (
    <p role="alert" className="font-semibold text-danger">
      {error instanceof ApiError ? t(messageKey(error.code), error.params) : t("state.error")}
    </p>
  );
}
