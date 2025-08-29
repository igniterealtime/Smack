#!/usr/bin/env bash
set -euo pipefail

: "${VM_TAG:=latest}"

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd)
SMACK_ROOT=$(realpath "${SCRIPT_DIR}/..")
cd "${SMACK_ROOT}"

CONTAINER_NAME=sinttest-ejabberd
ADMIN_PASS=sinttest

CONTAINER_STATUS=$(podman inspect --format='{{.State.Status}}' "${CONTAINER_NAME}" 2>/dev/null || true)
if [[ ${CONTAINER_STATUS} != "running" ]]; then
	export REGISTER_ADMIN_PASSWORD="${ADMIN_PASS}"
	VM_RUN_ARGS=(
		--name "${CONTAINER_NAME}"
		--detach
		--replace
		--env REGISTER_ADMIN_PASSWORD
	)

	PORTS=(
		1880
		1883
		5210
		5222
		5269
		5280
		5443
	)
	for PORT in "${PORTS[@]}"; do
		VM_RUN_ARGS+=(
			--publish ${PORT}:${PORT}
		)
	done

	VM_RUN_ARGS+=(
		ghcr.io/processone/ejabberd:"${VM_TAG}"
	)

	podman run "${VM_RUN_ARGS[@]}"
	sleep 3
fi

onexit() {
	if [[ ! -v KEEP_VM ]]; then
		podman stop "${CONTAINER_NAME}"
	fi
}
trap onexit EXIT

waitForPort() {
	timeout 30 bash -c \
			"until printf \"\" >/dev/tcp/localhost/${1}; do sleep 1; done"
}
if ! waitForPort 5222; then
	>&2 echo "Timeout while waiting for port 5222 to become available"
	exit 1
fi

SINTTEST_ARGS=(
	-Dsinttest.service=localhost
	-Dsinttest.acceptAllCertificates=true
	-Dsinttest.adminAccountUsername=admin
	-Dsinttest.adminAccountPassword="${ADMIN_PASS}"
	-Dsinttest.debugger=standard,console=off
)
./gradlew sinttest "${SINTTEST_ARGS[@]}" "${@}"
