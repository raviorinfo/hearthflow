const functions = require('firebase-functions');
const admin = require('firebase-admin');
const nodemailer = require('nodemailer');

admin.initializeApp();

// Create a Nodemailer transporter using Google Cloud Secret Manager variables
// To set these secrets, run:
// firebase functions:secrets:set GMAIL_EMAIL
// firebase functions:secrets:set GMAIL_APP_PASSWORD
const createTransporter = () => {
  return nodemailer.createTransport({
    service: 'gmail',
    auth: {
      user: process.env.GMAIL_EMAIL,
      pass: process.env.GMAIL_APP_PASSWORD,
    },
  });
};

// ==========================================
// Trigger 1: User Registration
// ==========================================
exports.onUserRegistration = functions
  .runWith({ secrets: ["GMAIL_EMAIL", "GMAIL_APP_PASSWORD"] })
  .database.ref('/users/{userKey}/profile')
  .onCreate((snapshot, context) => {
    const profile = snapshot.val();
    const email = profile.email;
    const name = profile.name || 'User';

    if (!email) return null;

    const mailOptions = {
      from: `"RoutineLog Support" <${process.env.GMAIL_EMAIL}>`,
      to: email,
      subject: 'Welcome to RoutineLog! 🎉',
      html: `
        <h2>Hi ${name}, welcome to RoutineLog!</h2>
        <p>We're thrilled to have you on board.</p>
        <p>Start logging your routines, splitting expenses securely, and managing your financial future today.</p>
        <br/>
        <p>Best regards,<br/>The RoutineLog Team</p>
      `,
    };

    return createTransporter().sendMail(mailOptions)
      .then(() => console.log('Welcome email sent to:', email))
      .catch((error) => console.error('Error sending welcome email:', error));
});

// ==========================================
// Trigger 2: Group Invitation
// ==========================================
// Note: This triggers when a user is added to a group.
exports.onGroupInvitation = functions
  .runWith({ secrets: ["GMAIL_EMAIL", "GMAIL_APP_PASSWORD"] })
  .database.ref('/users/{userKey}/joinedGroups/{groupId}')
  .onCreate(async (snapshot, context) => {
    const userKey = context.params.userKey;
    const groupId = context.params.groupId;

    try {
      // Fetch user profile to get their email
      const userSnap = await admin.database().ref(`/users/${userKey}/profile`).once('value');
      const userProfile = userSnap.val();
      if (!userProfile || !userProfile.email) return null;

      // Fetch group info
      const groupSnap = await admin.database().ref(`/groups/${groupId}/info`).once('value');
      const groupInfo = groupSnap.val();
      const groupName = groupInfo ? groupInfo.name : 'a new group';

      const mailOptions = {
        from: `"RoutineLog Team" <${process.env.GMAIL_EMAIL}>`,
        to: userProfile.email,
        subject: `You've been invited to ${groupName}!`,
        html: `
          <h2>Hello ${userProfile.name},</h2>
          <p>Great news! You have been added to the shared ledger group: <b>${groupName}</b>.</p>
          <p>Open your RoutineLog app to view the group and start splitting expenses.</p>
          <br/>
          <p>Best,<br/>The RoutineLog Team</p>
        `,
      };

      await createTransporter().sendMail(mailOptions);
      console.log('Group invite email sent to:', userProfile.email);
      return null;
    } catch (error) {
      console.error('Error sending group invite email:', error);
      return null;
    }
});

// ==========================================
// Trigger 3: App Invitation
// ==========================================
exports.onAppInvitation = functions
  .runWith({ secrets: ["GMAIL_EMAIL", "GMAIL_APP_PASSWORD"] })
  .database.ref('/app_invitations/{inviteId}')
  .onCreate((snapshot, context) => {
    const invite = snapshot.val();
    const inviteeEmail = invite.inviteeEmail;
    const senderName = invite.senderName || 'A friend';

    if (!inviteeEmail) return null;

    const mailOptions = {
      from: `"RoutineLog Team" <${process.env.GMAIL_EMAIL}>`,
      to: inviteeEmail,
      subject: `${senderName} invited you to RoutineLog!`,
      html: `
        <h2>Hi there!</h2>
        <p><b>${senderName}</b> has invited you to join RoutineLog—the ultimate secure hybrid financial ledger and daily planner.</p>
        <p>Get started today by downloading the app and taking control of your routines and finances.</p>
        <br/>
        <p>Best,<br/>The RoutineLog Team</p>
      `,
    };

    return createTransporter().sendMail(mailOptions)
      .then(() => console.log('App invitation email sent to:', inviteeEmail))
      .catch((error) => console.error('Error sending app invitation email:', error));
});
