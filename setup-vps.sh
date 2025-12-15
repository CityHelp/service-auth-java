#!/bin/bash

################################################################################
# CityHelp Auth Service - VPS Setup Script
#
# Purpose: Securely configure environment variables and deploy to VPS
#
# Usage:
#   ./setup-vps.sh --host <ip> --user <user> --password <pass> [options]
#
# Example:
#   ./setup-vps.sh \
#     --host 188.245.114.222 \
#     --user blinded \
#     --password "5423LEOn++" \
#     --app-base-url "http://188.245.114.222:8001" \
#     --support-email "support@cityhelp.com"
#
# Environment Variables (required in .env.local or passed as arguments):
#   DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD
#   REDIS_HOST, REDIS_PORT, REDIS_PASSWORD
#   JWT_SECRET, JWT_RSA_PRIVATE_KEY, JWT_RSA_PUBLIC_KEY
#   GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET
#   SMTP_HOST, SMTP_PORT, SMTP_USERNAME, SMTP_PASSWORD
#   SMTP_FROM_EMAIL, SMTP_FROM_NAME
#
# Features:
#   - Validates all required parameters
#   - Creates secure .env file on remote VPS
#   - Never exposes credentials in logs or stdout
#   - Supports dry-run mode for testing
#   - Comprehensive error handling
#
################################################################################

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
VPS_HOST=""
VPS_USER=""
VPS_PASSWORD=""
VPS_PORT="22"
VPS_DIR="/home/blinded/cityhelp-auth"
DRY_RUN=false
VERBOSE=false

# Application configuration
APP_BASE_URL=""
API_DOCS_DEV_URL="http://localhost:8001"
API_DOCS_PROD_URL=""
API_DOCS_CUSTOM_URL=""
SUPPORT_EMAIL="support@cityhelp.com"
DEV_EMAIL="dev@cityhelp.com"
GITHUB_REPO="https://github.com/cityhelp/auth-service"
TERMS_URL="https://cityhelp.com/terms"
OAUTH2_REDIRECT_URI=""

# Database
DB_HOST=""
DB_PORT="5433"
DB_NAME="cityhelp"
DB_USERNAME="root"
DB_PASSWORD=""

# Redis
REDIS_HOST=""
REDIS_PORT="11711"
REDIS_PASSWORD=""

# JWT
JWT_SECRET=""
JWT_RSA_PRIVATE_KEY=""
JWT_RSA_PUBLIC_KEY=""
JWT_KEY_ID="cityhelp-key-1"

# Google OAuth2
GOOGLE_CLIENT_ID=""
GOOGLE_CLIENT_SECRET=""
OAUTH2_GOOGLE_REDIRECT_URI=""

# SMTP
SMTP_HOST="smtp.gmail.com"
SMTP_PORT="587"
SMTP_USERNAME=""
SMTP_PASSWORD=""
SMTP_FROM_EMAIL=""
SMTP_FROM_NAME="CityHelp"

# Helper functions
print_header() {
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}\n"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

show_usage() {
    cat << EOF
Usage: $0 [OPTIONS]

Required Options:
  --host <ip>                 VPS IP address (e.g., 188.245.114.222)
  --user <username>           VPS SSH user (e.g., blinded)
  --password <password>       VPS SSH password

Application Configuration:
  --app-base-url <url>        Application base URL (e.g., http://188.245.114.222:8001)
  --support-email <email>     Support email (default: support@cityhelp.com)
  --dev-email <email>         Dev team email (default: dev@cityhelp.com)

Database Configuration:
  --db-host <host>           PostgreSQL host
  --db-port <port>           PostgreSQL port (default: 5433)
  --db-name <name>           Database name (default: cityhelp)
  --db-username <user>       Database user (default: root)
  --db-password <pass>       Database password

Redis Configuration:
  --redis-host <host>        Redis host
  --redis-port <port>        Redis port (default: 11711)
  --redis-password <pass>    Redis password

JWT Configuration:
  --jwt-secret <secret>      JWT secret key
  --jwt-private-key <key>    JWT RSA private key
  --jwt-public-key <key>     JWT RSA public key

OAuth2 Configuration:
  --google-client-id <id>     Google OAuth2 client ID
  --google-client-secret <s>  Google OAuth2 client secret

SMTP Configuration:
  --smtp-host <host>         SMTP host (default: smtp.gmail.com)
  --smtp-port <port>         SMTP port (default: 587)
  --smtp-username <user>     SMTP username
  --smtp-password <pass>     SMTP password
  --smtp-from-email <email>  From email address
  --smtp-from-name <name>    From name (default: CityHelp)

Optional Flags:
  --load-env <file>          Load variables from .env file first
  --dry-run                  Show what would be deployed without making changes
  --verbose                  Enable verbose output
  --help                     Show this help message

Examples:
  # Interactive mode (prompts for values)
  $0 --host 188.245.114.222 --user blinded --password "pass"

  # Load from .env.local
  $0 --load-env .env.local

  # Full configuration
  $0 --host 188.245.114.222 --user blinded --password "pass" \
     --app-base-url "http://188.245.114.222:8001" \
     --db-host 188.245.114.222 --db-password "securepass" \
     --redis-host redis.example.com --redis-password "redispass" \
     --jwt-secret "secret123" \
     --google-client-id "id123" --google-client-secret "secret123" \
     --smtp-username "email@gmail.com" --smtp-password "apppass"

EOF
    exit 0
}

# Parse command line arguments
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --host)
                VPS_HOST="$2"
                shift 2
                ;;
            --user)
                VPS_USER="$2"
                shift 2
                ;;
            --password)
                VPS_PASSWORD="$2"
                shift 2
                ;;
            --port)
                VPS_PORT="$2"
                shift 2
                ;;
            --app-base-url)
                APP_BASE_URL="$2"
                OAUTH2_REDIRECT_URI="${2}/oauth2/redirect"
                OAUTH2_GOOGLE_REDIRECT_URI="${2}/login/oauth2/code/google"
                API_DOCS_PROD_URL="$2"
                shift 2
                ;;
            --support-email)
                SUPPORT_EMAIL="$2"
                shift 2
                ;;
            --dev-email)
                DEV_EMAIL="$2"
                shift 2
                ;;
            --db-host)
                DB_HOST="$2"
                shift 2
                ;;
            --db-port)
                DB_PORT="$2"
                shift 2
                ;;
            --db-name)
                DB_NAME="$2"
                shift 2
                ;;
            --db-username)
                DB_USERNAME="$2"
                shift 2
                ;;
            --db-password)
                DB_PASSWORD="$2"
                shift 2
                ;;
            --redis-host)
                REDIS_HOST="$2"
                shift 2
                ;;
            --redis-port)
                REDIS_PORT="$2"
                shift 2
                ;;
            --redis-password)
                REDIS_PASSWORD="$2"
                shift 2
                ;;
            --jwt-secret)
                JWT_SECRET="$2"
                shift 2
                ;;
            --jwt-private-key)
                JWT_RSA_PRIVATE_KEY="$2"
                shift 2
                ;;
            --jwt-public-key)
                JWT_RSA_PUBLIC_KEY="$2"
                shift 2
                ;;
            --google-client-id)
                GOOGLE_CLIENT_ID="$2"
                shift 2
                ;;
            --google-client-secret)
                GOOGLE_CLIENT_SECRET="$2"
                shift 2
                ;;
            --smtp-host)
                SMTP_HOST="$2"
                shift 2
                ;;
            --smtp-port)
                SMTP_PORT="$2"
                shift 2
                ;;
            --smtp-username)
                SMTP_USERNAME="$2"
                shift 2
                ;;
            --smtp-password)
                SMTP_PASSWORD="$2"
                shift 2
                ;;
            --smtp-from-email)
                SMTP_FROM_EMAIL="$2"
                shift 2
                ;;
            --smtp-from-name)
                SMTP_FROM_NAME="$2"
                shift 2
                ;;
            --load-env)
                load_env_file "$2"
                shift 2
                ;;
            --dry-run)
                DRY_RUN=true
                shift
                ;;
            --verbose)
                VERBOSE=true
                shift
                ;;
            --help)
                show_usage
                ;;
            *)
                print_error "Unknown option: $1"
                show_usage
                ;;
        esac
    done
}

# Load environment variables from file
load_env_file() {
    local env_file="$1"
    if [[ ! -f "$env_file" ]]; then
        print_error "Environment file not found: $env_file"
        exit 1
    fi

    print_info "Loading environment from: $env_file"
    set -a
    source "$env_file"
    set +a
}

# Validate required parameters
validate_params() {
    local errors=0

    if [[ -z "$VPS_HOST" ]]; then
        print_error "Missing required parameter: --host"
        ((errors++))
    fi

    if [[ -z "$VPS_USER" ]]; then
        print_error "Missing required parameter: --user"
        ((errors++))
    fi

    if [[ -z "$VPS_PASSWORD" ]]; then
        print_error "Missing required parameter: --password"
        ((errors++))
    fi

    if [[ -z "$APP_BASE_URL" ]]; then
        print_error "Missing required parameter: --app-base-url"
        ((errors++))
    fi

    if [[ -z "$DB_HOST" ]]; then
        print_error "Missing required parameter: --db-host"
        ((errors++))
    fi

    if [[ -z "$DB_PASSWORD" ]]; then
        print_error "Missing required parameter: --db-password"
        ((errors++))
    fi

    if [[ -z "$REDIS_HOST" ]]; then
        print_error "Missing required parameter: --redis-host"
        ((errors++))
    fi

    if [[ -z "$REDIS_PASSWORD" ]]; then
        print_error "Missing required parameter: --redis-password"
        ((errors++))
    fi

    if [[ -z "$JWT_SECRET" ]]; then
        print_error "Missing required parameter: --jwt-secret"
        ((errors++))
    fi

    if [[ -z "$GOOGLE_CLIENT_ID" ]]; then
        print_error "Missing required parameter: --google-client-id"
        ((errors++))
    fi

    if [[ -z "$GOOGLE_CLIENT_SECRET" ]]; then
        print_error "Missing required parameter: --google-client-secret"
        ((errors++))
    fi

    if [[ -z "$SMTP_USERNAME" ]]; then
        print_error "Missing required parameter: --smtp-username"
        ((errors++))
    fi

    if [[ -z "$SMTP_PASSWORD" ]]; then
        print_error "Missing required parameter: --smtp-password"
        ((errors++))
    fi

    if [[ -z "$SMTP_FROM_EMAIL" ]]; then
        print_error "Missing required parameter: --smtp-from-email"
        ((errors++))
    fi

    if [[ $errors -gt 0 ]]; then
        echo ""
        print_error "Please provide all required parameters"
        show_usage
    fi
}

# Execute command on remote VPS
run_on_vps() {
    local cmd="$1"

    if [[ "$VERBOSE" == "true" ]]; then
        print_info "Running on VPS: $cmd"
    fi

    if [[ "$DRY_RUN" == "true" ]]; then
        return 0
    fi

    sshpass -p "$VPS_PASSWORD" ssh -o StrictHostKeyChecking=no -p "$VPS_PORT" \
        "$VPS_USER@$VPS_HOST" "$cmd" 2>/dev/null
}

# Generate .env file content
generate_env_file() {
    cat << 'ENVEOF'
# CityHelp Auth Service Environment Configuration
# Generated by setup-vps.sh - DO NOT COMMIT TO GIT
# This file contains sensitive credentials

################################################################################
# Spring Configuration
################################################################################

SPRING_PROFILE=prod
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8001

################################################################################
# Application URLs
################################################################################

APP_BASE_URL=__APP_BASE_URL__
API_DOCS_DEV_URL=__API_DOCS_DEV_URL__
API_DOCS_PROD_URL=__API_DOCS_PROD_URL__
API_DOCS_CUSTOM_URL=__API_DOCS_CUSTOM_URL__
OAUTH2_REDIRECT_URI=__OAUTH2_REDIRECT_URI__
OAUTH2_GOOGLE_REDIRECT_URI=__OAUTH2_GOOGLE_REDIRECT_URI__

################################################################################
# Contact Information
################################################################################

SUPPORT_EMAIL=__SUPPORT_EMAIL__
DEV_EMAIL=__DEV_EMAIL__
GITHUB_REPO=__GITHUB_REPO__
TERMS_URL=__TERMS_URL__

################################################################################
# PostgreSQL Database Configuration
################################################################################

DB_HOST=__DB_HOST__
DB_PORT=__DB_PORT__
DB_NAME=__DB_NAME__
DB_USERNAME=__DB_USERNAME__
DB_PASSWORD=__DB_PASSWORD__

################################################################################
# Redis Configuration (Redis Labs)
################################################################################

REDIS_HOST=__REDIS_HOST__
REDIS_PORT=__REDIS_PORT__
REDIS_PASSWORD=__REDIS_PASSWORD__

################################################################################
# JWT Configuration
################################################################################

JWT_SECRET=__JWT_SECRET__
JWT_RSA_PRIVATE_KEY=__JWT_RSA_PRIVATE_KEY__
JWT_RSA_PUBLIC_KEY=__JWT_RSA_PUBLIC_KEY__
JWT_KEY_ID=__JWT_KEY_ID__

################################################################################
# Google OAuth2 Configuration
################################################################################

GOOGLE_CLIENT_ID=__GOOGLE_CLIENT_ID__
GOOGLE_CLIENT_SECRET=__GOOGLE_CLIENT_SECRET__

################################################################################
# SMTP Email Configuration
################################################################################

SMTP_HOST=__SMTP_HOST__
SMTP_PORT=__SMTP_PORT__
SMTP_USERNAME=__SMTP_USERNAME__
SMTP_PASSWORD=__SMTP_PASSWORD__
SMTP_FROM_EMAIL=__SMTP_FROM_EMAIL__
SMTP_FROM_NAME=__SMTP_FROM_NAME__

################################################################################
# Environment
################################################################################

ENVIRONMENT=production
PASSWORD_RESET_EXPIRATION_HOURS=1
ENVEOF
}

# Replace placeholders in .env file
substitute_env_vars() {
    local content="$1"

    content="${content//__APP_BASE_URL__/$APP_BASE_URL}"
    content="${content//__API_DOCS_DEV_URL__/$API_DOCS_DEV_URL}"
    content="${content//__API_DOCS_PROD_URL__/$API_DOCS_PROD_URL}"
    content="${content//__API_DOCS_CUSTOM_URL__/$API_DOCS_CUSTOM_URL}"
    content="${content//__OAUTH2_REDIRECT_URI__/$OAUTH2_REDIRECT_URI}"
    content="${content//__OAUTH2_GOOGLE_REDIRECT_URI__/$OAUTH2_GOOGLE_REDIRECT_URI}"

    content="${content//__SUPPORT_EMAIL__/$SUPPORT_EMAIL}"
    content="${content//__DEV_EMAIL__/$DEV_EMAIL}"
    content="${content//__GITHUB_REPO__/$GITHUB_REPO}"
    content="${content//__TERMS_URL__/$TERMS_URL}"

    content="${content//__DB_HOST__/$DB_HOST}"
    content="${content//__DB_PORT__/$DB_PORT}"
    content="${content//__DB_NAME__/$DB_NAME}"
    content="${content//__DB_USERNAME__/$DB_USERNAME}"
    content="${content//__DB_PASSWORD__/$DB_PASSWORD}"

    content="${content//__REDIS_HOST__/$REDIS_HOST}"
    content="${content//__REDIS_PORT__/$REDIS_PORT}"
    content="${content//__REDIS_PASSWORD__/$REDIS_PASSWORD}"

    content="${content//__JWT_SECRET__/$JWT_SECRET}"
    content="${content//__JWT_RSA_PRIVATE_KEY__/$JWT_RSA_PRIVATE_KEY}"
    content="${content//__JWT_RSA_PUBLIC_KEY__/$JWT_RSA_PUBLIC_KEY}"
    content="${content//__JWT_KEY_ID__/$JWT_KEY_ID}"

    content="${content//__GOOGLE_CLIENT_ID__/$GOOGLE_CLIENT_ID}"
    content="${content//__GOOGLE_CLIENT_SECRET__/$GOOGLE_CLIENT_SECRET}"

    content="${content//__SMTP_HOST__/$SMTP_HOST}"
    content="${content//__SMTP_PORT__/$SMTP_PORT}"
    content="${content//__SMTP_USERNAME__/$SMTP_USERNAME}"
    content="${content//__SMTP_PASSWORD__/$SMTP_PASSWORD}"
    content="${content//__SMTP_FROM_EMAIL__/$SMTP_FROM_EMAIL}"
    content="${content//__SMTP_FROM_NAME__/$SMTP_FROM_NAME}"

    echo "$content"
}

# Deploy to VPS
deploy_to_vps() {
    print_header "Deploying to VPS"

    print_info "VPS Configuration:"
    echo "  Host: $VPS_HOST"
    echo "  User: $VPS_USER"
    echo "  Directory: $VPS_DIR"
    echo ""

    # Step 1: Create directories
    print_info "Step 1/4: Creating directories on VPS..."
    run_on_vps "mkdir -p $VPS_DIR && cd $VPS_DIR && pwd"
    print_success "Directories created"

    # Step 2: Upload .env file securely
    print_info "Step 2/4: Generating and uploading .env file..."
    local env_content=$(generate_env_file)
    env_content=$(substitute_env_vars "$env_content")

    if [[ "$DRY_RUN" == "true" ]]; then
        echo ""
        print_warning "DRY RUN - Would create .env with following content:"
        echo "$env_content" | head -20
        echo "... (truncated for security)"
    else
        # Create temporary file and upload
        local temp_env=$(mktemp)
        echo "$env_content" > "$temp_env"
        chmod 600 "$temp_env"

        sshpass -p "$VPS_PASSWORD" scp -o StrictHostKeyChecking=no -P "$VPS_PORT" \
            "$temp_env" "$VPS_USER@$VPS_HOST:$VPS_DIR/.env" 2>/dev/null

        rm "$temp_env"

        # Verify upload
        run_on_vps "test -f $VPS_DIR/.env && echo 'OK' || echo 'FAILED'" > /dev/null
    fi
    print_success ".env file uploaded securely"

    # Step 3: Pull latest code
    print_info "Step 3/4: Pulling latest code from repository..."
    run_on_vps "cd $VPS_DIR && git pull origin dev 2>/dev/null || echo 'Not a git repo'"
    print_success "Code updated"

    # Step 4: Start services
    print_info "Step 4/4: Starting Docker services..."
    run_on_vps "cd $VPS_DIR && docker-compose -f docker-compose.vps.yml down 2>/dev/null || true"
    run_on_vps "cd $VPS_DIR && docker-compose -f docker-compose.vps.yml up -d"

    sleep 5
    run_on_vps "docker ps | grep cityhelp-auth-service" > /dev/null 2>&1
    print_success "Docker services started"

    # Verify health
    print_info "Verifying service health..."
    sleep 10
    local health_check=$(sshpass -p "$VPS_PASSWORD" ssh -o StrictHostKeyChecking=no -p "$VPS_PORT" \
        "$VPS_USER@$VPS_HOST" "curl -s http://localhost:8001/actuator/health 2>/dev/null | grep -o '\"status\":\"UP\"' || echo 'DOWN'")

    if [[ "$health_check" == *"UP"* ]]; then
        print_success "Service is healthy and responding"
    else
        print_warning "Service health check inconclusive - checking container logs..."
        run_on_vps "docker-compose -f $VPS_DIR/docker-compose.vps.yml logs --tail=20 auth-service"
    fi
}

# Show summary
show_summary() {
    print_header "Deployment Summary"

    print_success "Setup completed!"
    echo ""
    echo "VPS Information:"
    echo "  Host: $VPS_HOST"
    echo "  User: $VPS_USER"
    echo "  Deploy Directory: $VPS_DIR"
    echo ""
    echo "Service Information:"
    echo "  Base URL: $APP_BASE_URL"
    echo "  Health Check: $APP_BASE_URL/actuator/health"
    echo "  JWKS Endpoint: $APP_BASE_URL/.well-known/jwks.json"
    echo "  API Docs: $APP_BASE_URL/v3/api-docs"
    echo ""
    echo "Useful Commands:"
    echo "  View logs:"
    echo "    sshpass -p '$VPS_PASSWORD' ssh -o StrictHostKeyChecking=no $VPS_USER@$VPS_HOST 'cd $VPS_DIR && docker-compose -f docker-compose.vps.yml logs -f auth-service'"
    echo ""
    echo "  Stop service:"
    echo "    sshpass -p '$VPS_PASSWORD' ssh -o StrictHostKeyChecking=no $VPS_USER@$VPS_HOST 'cd $VPS_DIR && docker-compose -f docker-compose.vps.yml down'"
    echo ""
    echo "  Restart service:"
    echo "    sshpass -p '$VPS_PASSWORD' ssh -o StrictHostKeyChecking=no $VPS_USER@$VPS_HOST 'cd $VPS_DIR && docker-compose -f docker-compose.vps.yml restart auth-service'"
    echo ""
}

# Main execution
main() {
    print_header "CityHelp Auth Service - VPS Setup"

    # Parse arguments
    parse_args "$@"

    # Validate parameters
    validate_params

    # Deploy
    deploy_to_vps

    # Summary
    show_summary
}

# Run main function
main "$@"
