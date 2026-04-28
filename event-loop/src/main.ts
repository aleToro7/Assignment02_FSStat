import { FsStatScanner } from "#core/scanner";

const subdirs: string[] = await FsStatScanner().getSubdirectories("./src");

console.log(subdirs.join(", "));
