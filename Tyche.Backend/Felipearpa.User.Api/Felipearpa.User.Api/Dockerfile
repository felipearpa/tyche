FROM mcr.microsoft.com/dotnet/aspnet:7.0 AS base
WORKDIR /app
EXPOSE 80
EXPOSE 443

FROM mcr.microsoft.com/dotnet/sdk:7.0 AS build
WORKDIR /src
COPY ["Felipearpa.User.Api/Felipearpa.User.Api.fsproj", "Felipearpa.User.Api/"]
RUN dotnet restore "Felipearpa.User.Api/Felipearpa.User.Api.fsproj"
COPY . .
WORKDIR "/src/Felipearpa.User.Api"
RUN dotnet build "Felipearpa.User.Api.fsproj" -c Release -o /app/build

FROM build AS publish
RUN dotnet publish "Felipearpa.User.Api.fsproj" -c Release -o /app/publish

FROM base AS final
WORKDIR /app
COPY --from=publish /app/publish .
ENTRYPOINT ["dotnet", "Felipearpa.User.Api.dll"]
