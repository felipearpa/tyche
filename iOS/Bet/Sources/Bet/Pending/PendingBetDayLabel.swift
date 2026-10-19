import Foundation

/// A Bets date header's text, relative to `now` in the calendar's time zone: Today, Tomorrow, or
/// Yesterday; the weekday name for the other days of the next six; otherwise a short named date
/// with weekday, day, and month, and the year only when it is not the current one. Formats come
/// from the locale, capitalized as a heading ("Miércoles", not "miércoles").
struct PendingBetDayLabel {
    let calendar: Calendar
    let locale: Locale

    init(calendar: Calendar = .current, locale: Locale = .current) {
        self.calendar = calendar
        self.locale = locale
    }

    func text(for day: Date, now: Date) -> String {
        switch daysFromToday(to: day, now: now) {
        case 0: return localized(.betDayToday)
        case 1: return localized(.betDayTomorrow)
        case -1: return localized(.betDayYesterday)
        case 2...6: return day.formatted(style.weekday(.wide))
        default: return day.formatted(shortDateStyle(for: day, now: now))
        }
    }

    private func daysFromToday(to day: Date, now: Date) -> Int {
        let today = calendar.startOfDay(for: now)
        let kickoffDay = calendar.startOfDay(for: day)
        return calendar.dateComponents([.day], from: today, to: kickoffDay).day ?? 0
    }

    private func shortDateStyle(for day: Date, now: Date) -> Date.FormatStyle {
        let shortDate = style.weekday(.abbreviated).day().month(.abbreviated)
        let isSameYear = calendar.component(.year, from: day) == calendar.component(.year, from: now)
        return isSameYear ? shortDate : shortDate.year()
    }

    private var style: Date.FormatStyle {
        Date.FormatStyle(
            date: .omitted,
            time: .omitted,
            locale: locale,
            calendar: calendar,
            timeZone: calendar.timeZone,
            capitalizationContext: .beginningOfSentence
        )
    }

    private func localized(_ resource: LocalizedStringResource) -> String {
        var resource = resource
        resource.locale = locale
        return String(localized: resource)
    }
}
