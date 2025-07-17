import SwiftUI

import SwiftUI

struct PoolFromLayoutCreatorStepTwoView: View {
    let createPoolModel: CreatePoolModel
    let onSaveClick: (CreatePoolModel) -> Void

    @Environment(\.boxSpacing) private var boxSpacing
    @State private var poolName: String

    init(
        createPoolModel: CreatePoolModel,
        onSaveClick: @escaping (CreatePoolModel) -> Void
    ) {
        self.createPoolModel = createPoolModel
        self.onSaveClick = onSaveClick
        self._poolName = State(initialValue: createPoolModel.poolName)
    }

    var isValid: Bool {
        !poolName.isEmpty
    }

    var body: some View {
        VStack(spacing: boxSpacing.large) {
            HStack {
                Text(String(.fillPoolFieldsTitle))
                    .font(.title3)
                Spacer()
            }

            FormBody(
                poolName: poolName,
                onPoolNameChange: { poolName = $0 }
            )

            Button(action: {
                onSaveClick(createPoolModel.copy { builder in builder.poolName = poolName })
            }) {
                HStack(spacing: boxSpacing.small) {
                    Image(sharedResource: .done)
                    Text(String(sharedResource: .doneAction))
                }
                .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .disabled(!isValid)

            Spacer()
        }
    }
}

private struct FormBody: View {
    let poolName: String
    let onPoolNameChange: (String) -> Void

    var body: some View {
        PoolNameTextField(
            value: poolName,
            onValueChange: onPoolNameChange
        )
    }
}

private struct PoolNameTextField: View {
    let value: String
    let onValueChange: (String) -> Void

    @Environment(\.boxSpacing) private var boxSpacing
    @FocusState private var isFocused: Bool

    var shouldShowError: Bool {
        isFocused && value.isEmpty
    }

    var body: some View {
        VStack(spacing: boxSpacing.small) {
            TextField(
                String(.poolNameLabel),
                text: Binding(
                    get: { value },
                    set: { onValueChange($0) }
                )
            )
            .textFieldStyle(.roundedBorder)
            .focused($isFocused)
            .submitLabel(.done)

            if shouldShowError {
                Text(String(.poolNameRequiredFailureText))
                    .foregroundColor(.red)
                    .font(.footnote)
            }
        }
    }
}

#Preview {
    PoolFromLayoutCreatorStepTwoView(
        createPoolModel: emptyCreatePoolModel(),
        onSaveClick: { _ in }
    )
    .padding()
}
