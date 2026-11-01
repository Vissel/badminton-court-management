import { displayToIso, isoToDisplay, maskDateInput } from "./DateInput";

describe("DateInput dd/mm/yyyy helpers", () => {
    test("isoToDisplay renders day first", () => {
        expect(isoToDisplay("2026-11-03")).toBe("03/11/2026");
        expect(isoToDisplay("2026-01-09")).toBe("09/01/2026");
        expect(isoToDisplay("")).toBe("");
        expect(isoToDisplay(undefined)).toBe("");
        expect(isoToDisplay("not-a-date")).toBe("");
        expect(isoToDisplay("2026-11-03T10:00:00")).toBe("");
    });

    test("displayToIso converts back to the ISO value the API expects", () => {
        expect(displayToIso("03/11/2026")).toBe("2026-11-03");
        expect(displayToIso("3/11/2026")).toBe("2026-11-03");
        expect(displayToIso("03/11/2026 ")).toBe("2026-11-03");
    });

    test("displayToIso rejects impossible or incomplete dates", () => {
        expect(displayToIso("31/02/2026")).toBeNull();
        expect(displayToIso("03/13/2026")).toBeNull();
        expect(displayToIso("00/11/2026")).toBeNull();
        expect(displayToIso("03/11/26")).toBeNull();
        expect(displayToIso("03/11/1899")).toBeNull();
        expect(displayToIso("")).toBeNull();
        expect(displayToIso("abc")).toBeNull();
    });

    test("maskDateInput separates a digit run into dd/mm/yyyy", () => {
        expect(maskDateInput("03112026")).toBe("03/11/2026");
        expect(maskDateInput("0311")).toBe("03/11");
        expect(maskDateInput("031")).toBe("03/1");
        expect(maskDateInput("03")).toBe("03");
        expect(maskDateInput("0311202699")).toBe("03/11/2026");
    });

    test("maskDateInput keeps separators the user typed or pasted", () => {
        expect(maskDateInput("03/11/2026")).toBe("03/11/2026");
        expect(maskDateInput("3/11/2026")).toBe("3/11/2026");
        expect(maskDateInput("03/11/2026extra")).toBe("03/11/2026");
        expect(maskDateInput("")).toBe("");
    });
});
