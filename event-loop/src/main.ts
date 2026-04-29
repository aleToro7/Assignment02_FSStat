import { FsStatScanner } from "#core/scanner";
import type { FsStatReport } from "#types/index";
import { benchmark } from "#utils/benchmark";
import { FileSize } from "#utils/sizes";

const dirPath = process.argv[2] ?? "/home/diottanax/";

const scanner = FsStatScanner();
const nb = 5;
const maxFileSize = FileSize.megabyte(100);
const { result, duration } = await benchmark(() =>
  scanner.getReport(dirPath, maxFileSize, nb),
);
const report: FsStatReport = result.dto();
console.log(`Report: ${JSON.stringify(report, null, 2)}`);
console.log(`Time taken: ${duration.toFixed(2)} ms`);
