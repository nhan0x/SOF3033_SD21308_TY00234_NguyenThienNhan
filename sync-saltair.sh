#!/usr/bin/env bash
# Đồng bộ code từ repo dự án Saltair (repo nhóm) sang repo SOF309 này.
# Chép: src/main (web), src/test/java/com/fashionweb (test của Saltair), database/ (SQL).
# Chỉ chép MỘT CHIỀU Saltair -> SOF309. Repo Saltair không bị thay đổi.
#
# Cách dùng (chạy ở bất kỳ thư mục nào):
#   ./sync-saltair.sh            chép đè file mới/đã sửa, giữ file thừa bên SOF309
#   ./sync-saltair.sh --delete   như trên + xóa bên SOF309 những file Saltair đã xóa
#   SALTAIR_DIR=/duong/dan/khac ./sync-saltair.sh   nếu Saltair nằm ở chỗ khác
set -euo pipefail

SOF_DIR="$(cd "$(dirname "$0")" && pwd)"
SALTAIR_DIR="${SALTAIR_DIR:-$HOME/Documents/Saltair/saltair-fashion-web}"

if [[ ! -f "$SALTAIR_DIR/pom.xml" ]]; then
  echo "Không tìm thấy project Saltair ở: $SALTAIR_DIR" >&2
  exit 1
fi

OPTS=(-a --itemize-changes --exclude='.DS_Store')
TEST_OPTS=(-a --itemize-changes --exclude='.DS_Store')
if [[ "${1:-}" == "--delete" ]]; then
  TEST_OPTS+=(--delete)
  # Giữ lại file riêng của template SOF309 trong src/main/java
  OPTS+=(--delete --exclude='/java/.gitkeep' --exclude='/java/Student.java')
fi

echo "Saltair : $SALTAIR_DIR ($(git -C "$SALTAIR_DIR" branch --show-current 2>/dev/null || echo '?'))"
echo "SOF309  : $SOF_DIR"
echo

rsync "${OPTS[@]}" "$SALTAIR_DIR/src/main/" "$SOF_DIR/src/main/"
if [[ -d "$SALTAIR_DIR/src/test/java/com/fashionweb" ]]; then
  mkdir -p "$SOF_DIR/src/test/java/com/fashionweb"
  rsync "${TEST_OPTS[@]}" "$SALTAIR_DIR/src/test/java/com/fashionweb/" "$SOF_DIR/src/test/java/com/fashionweb/"
fi
rsync -a --itemize-changes --exclude='.DS_Store' "$SALTAIR_DIR/database/" "$SOF_DIR/database/"

echo
echo "Xong. Xem thay đổi bằng: git -C \"$SOF_DIR\" status"
echo "Lưu ý: pom.xml KHÔNG được chép (SOF309 có thư viện test riêng). Nếu Saltair thêm thư viện mới, hãy thêm tay vào pom.xml của SOF309."
