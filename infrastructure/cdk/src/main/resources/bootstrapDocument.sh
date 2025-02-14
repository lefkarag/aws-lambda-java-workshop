bash << 'HEREDOC'
set -e
set -o xtrace

echo "Retrieving IDE password..."

PASSWORD_SECRET_VALUE=$(aws secretsmanager get-secret-value --secret-id "${passwordName}" --query 'SecretString' --output text)
export IDE_PASSWORD=$(echo "$PASSWORD_SECRET_VALUE" | jq -r '.password')

echo "Setting profile variables..."

# Set some useful variables
export TOKEN=$(curl -X PUT "http://169.254.169.254/latest/api/token" -H "X-aws-ec2-metadata-token-ttl-seconds: 21600")
export AWS_REGION=$(curl -H "X-aws-ec2-metadata-token: $TOKEN" -s http://169.254.169.254/latest/dynamic/instance-identity/document | grep region | awk -F\" '{print $4}')
export EC2_PRIVATE_IP=$(curl -H "X-aws-ec2-metadata-token: $TOKEN" -s http://169.254.169.254/latest/meta-data/local-ipv4)

echo "Installing AWS CLI..."

# Install AWS CLI
curl -L -o /tmp/aws-cli.zip https://awscli.amazonaws.com/awscli-exe-linux-$(uname -m).zip
unzip -d /tmp /tmp/aws-cli.zip
/tmp/aws/install --update
rm -rf /tmp/aws

echo "Installing Docker..."

# Install docker and base package
dnf install -y docker git
service docker start
usermod -aG docker ec2-user

echo "Installing code-server..."

# Install code-server
codeServer=$(dnf list installed code-server | wc -l)
if [ "$codeServer" -eq "0" ]; then
  sudo -u ec2-user "codeServerVersion=${codeServerVersion}" bash -c 'curl -fsSL https://code-server.dev/install.sh | sh -s -- --version ${codeServerVersion}'
  systemctl enable --now code-server@ec2-user
fi

sudo -u ec2-user bash -c 'mkdir -p ~/.config/code-server'
sudo -u ec2-user bash -c 'touch ~/.config/code-server/config.yaml'
tee /home/ec2-user/.config/code-server/config.yaml <<EOF
cert: false
auth: password
password: "$IDE_PASSWORD"
bind-addr: 127.0.0.1:8889
EOF

echo "Configuring code-server..."

sudo -u ec2-user bash -c 'mkdir -p ~/.local/share/code-server/User'
sudo -u ec2-user bash -c 'touch ~/.local/share/code-server/User/settings.json'
tee /home/ec2-user/.local/share/code-server/User/settings.json <<EOF
{
  "extensions.autoUpdate": false,
  "extensions.autoCheckUpdates": false,
  "security.workspace.trust.enabled": false,
  "workbench.startupEditor": "terminal",
  "task.allowAutomaticTasks": "on",
  "telemetry.telemetryLevel": "off",
  "update.mode": "none",
  "update.showReleaseNotes": false
}
EOF

echo "Restarting code-server..."

systemctl restart code-server@ec2-user

echo "Installing Caddy..."

# Install caddy
dnf copr enable -y @caddy/caddy epel-9-x86_64
dnf install -y caddy
systemctl enable --now caddy

tee /etc/caddy/Caddyfile <<EOF
http://${domain} {
  reverse_proxy 127.0.0.1:8889
}
EOF

echo "Restarting caddy..."

systemctl restart caddy

# Create default directory for workspace
sudo -u ec2-user bash -c 'mkdir -p ~/environment'

if [ ! -f "/home/ec2-user/.local/share/code-server/coder.json" ]; then
  sudo -u ec2-user bash -c 'touch ~/.local/share/code-server/coder.json'
  echo '{ "query": { "folder": "/home/ec2-user/environment" } }' > /home/ec2-user/.local/share/code-server/coder.json
fi

echo "Running custom bootstrap script..."

${customBootstrapScript}
HEREDOC

exit_code=$?

/opt/aws/bin/cfn-signal -e $exit_code '${waitConditionHandleUrl}'

exit $exit_code