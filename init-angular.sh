#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# init-angular.sh
# Génère le projet Angular "fraud-dashboard" et y installe le PaymentService.
#
# Prérequis : Node.js >= 18, Angular CLI installé globalement
#   npm install -g @angular/cli
#
# Usage :
#   chmod +x init-angular.sh
#   ./init-angular.sh
# ---------------------------------------------------------------------------
set -euo pipefail

PROJECT_NAME="fraud-dashboard"

echo ">>> Génération du projet Angular : ${PROJECT_NAME}"
ng new "${PROJECT_NAME}" \
  --routing=false \
  --style=scss \
  --skip-git \
  --no-interactive

echo ">>> Installation du PaymentService"
cp payment.service.ts "${PROJECT_NAME}/src/app/payment.service.ts"

echo ""
echo ">>> Terminé. Pour démarrer le frontend :"
echo "    cd ${PROJECT_NAME} && ng serve --proxy-config proxy.conf.json"
echo ""
echo "    (proxy.conf.json à créer pour rediriger /api vers http://localhost:8080)"
