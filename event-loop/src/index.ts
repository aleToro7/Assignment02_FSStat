import { FsStatScanner } from "#core/scanner";

const scanner = FsStatScanner();
const getFSReport = scanner.getReport.bind(scanner);
const fsstat = { getFSReport };

export default { fsstat };
export { getFSReport };
