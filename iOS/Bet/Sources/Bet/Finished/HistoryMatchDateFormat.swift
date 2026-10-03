import Foundation

/// The date and time shown in a History row. The date names the weekday, abbreviated month, and
/// day in the locale's order, and adds the year only when the match falls outside the current
/// calendar year. The time follows the locale and the device's 12/24-hour setting.
struct HistoryMatchDateFormat {
    let locale: Locale
    let calendar: Calendar
    let timeZone: TimeZone

    init(
        locale: Locale = .autoupdatingCurrent,
        calendar: Calendar = .autoupdatingCurrent,
        timeZone: TimeZone = .autoupdatingCurrent
    ) {
        self.locale = locale
        self.calendar = calendar
        self.timeZone = timeZone
    }

    func date(_ date: Date, now: Date = Date()) -> String {
        let style = Date.FormatStyle(locale: locale, calendar: calendar, timeZone: timeZone)
            .weekday(.wide)
            .month(.abbreviated)
            .day()
        return isInCurrentYear(date, now: now)
            ? date.formatted(style)
            : date.formatted(style.year())
    }

    func time(_ date: Date) -> String {
        date.formatted(
            Date.FormatStyle(locale: locale, calendar: calendar, timeZone: timeZone)
                .hour()
                .minute()
        )
    }

    private func isInCurrentYear(_ date: Date, now: Date) -> Bool {
        var calendar = calendar
        calendar.timeZone = timeZone
        return calendar.component(.year, from: date) == calendar.component(.year, from: now)
    }
}
