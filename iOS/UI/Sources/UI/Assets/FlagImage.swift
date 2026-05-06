import SwiftUI

public struct FlagImage: View {
    private let teamCode: String

    public init(teamCode: String) {
        self.teamCode = teamCode
    }

    public var body: some View {
        if let resource = flagResource(for: teamCode) {
            Image(sharedResource: resource)
                .resizable()
                .scaledToFit()
        }
    }
}

private func flagResource(for teamCode: String) -> SharedImageResource? {
    switch teamCode.lowercased() {
    case "ar": return SharedImageResource(.flagAr)
    case "at": return SharedImageResource(.flagAt)
    case "au": return SharedImageResource(.flagAu)
    case "ba": return SharedImageResource(.flagBa)
    case "be": return SharedImageResource(.flagBe)
    case "br": return SharedImageResource(.flagBr)
    case "ca": return SharedImageResource(.flagCa)
    case "cd": return SharedImageResource(.flagCd)
    case "ch": return SharedImageResource(.flagCh)
    case "ci": return SharedImageResource(.flagCi)
    case "co": return SharedImageResource(.flagCo)
    case "cv": return SharedImageResource(.flagCv)
    case "cw": return SharedImageResource(.flagCw)
    case "cz": return SharedImageResource(.flagCz)
    case "de": return SharedImageResource(.flagDe)
    case "dz": return SharedImageResource(.flagDz)
    case "ec": return SharedImageResource(.flagEc)
    case "eg": return SharedImageResource(.flagEg)
    case "es": return SharedImageResource(.flagEs)
    case "fr": return SharedImageResource(.flagFr)
    case "gb_eng": return SharedImageResource(.flagGbEng)
    case "gb_sct": return SharedImageResource(.flagGbSct)
    case "gh": return SharedImageResource(.flagGh)
    case "hr": return SharedImageResource(.flagHr)
    case "ht": return SharedImageResource(.flagHt)
    case "iq": return SharedImageResource(.flagIq)
    case "ir": return SharedImageResource(.flagIr)
    case "jo": return SharedImageResource(.flagJo)
    case "jp": return SharedImageResource(.flagJp)
    case "kr": return SharedImageResource(.flagKr)
    case "ma": return SharedImageResource(.flagMa)
    case "mx": return SharedImageResource(.flagMx)
    case "nl": return SharedImageResource(.flagNl)
    case "no": return SharedImageResource(.flagNo)
    case "nz": return SharedImageResource(.flagNz)
    case "pa": return SharedImageResource(.flagPa)
    case "pt": return SharedImageResource(.flagPt)
    case "py": return SharedImageResource(.flagPy)
    case "qa": return SharedImageResource(.flagQa)
    case "sa": return SharedImageResource(.flagSa)
    case "se": return SharedImageResource(.flagSe)
    case "sn": return SharedImageResource(.flagSn)
    case "tn": return SharedImageResource(.flagTn)
    case "tr": return SharedImageResource(.flagTr)
    case "us": return SharedImageResource(.flagUs)
    case "uy": return SharedImageResource(.flagUy)
    case "uz": return SharedImageResource(.flagUz)
    case "za": return SharedImageResource(.flagZa)
    default: return nil
    }
}
