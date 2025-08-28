import SwiftUI
import UI
import DataPool

struct PoolFromLayoutCreatorStepTwoView: View {
    let createPoolModel: CreatePoolModel
    let onSaveClick: (CreatePoolModel) -> Void

    @Environment(\.boxSpacing) private var boxSpacing
    @State private var poolName: String

    var isValid: Bool {
        PoolName.isValid(poolName)
    }

    init(
        createPoolModel: CreatePoolModel,
        onSaveClick: @escaping (CreatePoolModel) -> Void
    ) {
        self.createPoolModel = createPoolModel
        self.onSaveClick = onSaveClick
        self._poolName = State(initialValue: createPoolModel.poolName)
    }

    var body: some View {
        VStack(spacing: boxSpacing.large) {
            HStack {
                Text(String(.fillPoolFieldsTitle))
                    .font(.title3)
                Spacer()
            }

            FormBody(poolName: $poolName, isValid: isValid)

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
    @Binding var poolName: String
    let isValid: Bool

    var body: some View {
        PoolNameTextField(value: $poolName, isValid: isValid)
    }
}

private struct PoolNameTextField: View {
    @Binding var value: String
    let isValid: Bool

    @Environment(\.boxSpacing) private var boxSpacing
    @FocusState private var isFocused: Bool

    var body: some View {
        ValidatableTextField(
            label: String(.poolNameLabel),
            value: $value,
            isValid: isValid,
            errorMessage: String(.poolNameLengthValidationError),
        )
        .textFieldStyle(.roundedBorder)
    }
}

#Preview {
    PoolFromLayoutCreatorStepTwoView(
        createPoolModel: emptyCreatePoolModel(),
        onSaveClick: { _ in }
    )
    .padding()
}
