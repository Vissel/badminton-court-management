import React, { useEffect, useRef, useState } from "react";
import { DndProvider } from "react-dnd";
import { HTML5Backend } from "react-dnd-html5-backend";
import "../App.css";
import api from "../api/index";

import DraggableService from "./dragNdrop/DraggableService";
import Court from "./dragNdrop/Court";
import PlayerArea from "./dragNdrop/PlayerArea";
import ServiceDialog from "./dialog/ServiceDialog";
import GameDialog from "./dialog/GameDialog";
import ShuttleBallDialog from "./dialog/ShuttleBallDialog";

const COST_IN_PERSON = "costInPerson";
const VN_COST_IN_PERSON = "Tiền sân";
/* HomePage */
function HomePage() {
  const courtIds = [1, 2, 3, 4, 5, 6, 7];

  // Sort images for the right column (1, 2, 3) to display from bottom-up visually
  // We need to reverse the order for rendering to achieve "right-end > up"
  const rightColumn = courtIds
    .filter((id) => id >= 1 && id <= 3)
    .sort((a, b) => a - b); // Ensure 1, 2, 3 order, then reverse for display

  // Sort images for the left column (4, 5, 6, 7)
  const leftColumn = courtIds
    .filter((id) => id >= 4 && id <= 7)
    .sort((a, b) => a - b);

  const [courts, setCourts] = useState(() => {
    const initialCourts = {};
    courtIds.forEach((id) => {
      initialCourts[id] = { A: null, B: null, C: null, D: null };
    });
    return initialCourts;
  });

  // shuttle_ball selection
  const [selectedBall, setSelectedBall] = useState("");
  const selectedBallRef = useRef("");
  const [ballOptions, setBallOptions] = useState([]);
  const [services, setServices] = useState([]);
  const [costInPerson, setCostInPerson] = useState();
  const [availablePlayers, setAvailablePlayers] = useState([]);
  const [newPlayer, setNewPlayer] = useState("");
  const scrollRef = useRef(null);
  // const [courts, setCourts] = useState();

  const [showGameDialog, setShowGameDialog] = useState(false);
  const [gameDialogData, setGameDialogData] = useState("");

  const responseSuccess = (response) => {
    return response.status === 200 && response.data != null;
  };
  const responseDataTrue = (response) => {
    return responseSuccess(response.status) && response.data === true;
  };

  // Get shuttleName
  const getShuttleBallName = (ballString) => {
    return ballString.slice(0, ballString.lastIndexOf("-")).trim();
  };
  const getShuttleBallCost = (ballString) => {
    return Number(ballString.slice(ballString.lastIndexOf("-") + 1).trim());
  };

  // 
  const  handleSelectedBall = (ballChangeOption)=>{
    api.post(`/court-mana/changeSelectedBall`,{
      shuttleName: getShuttleBallName(ballChangeOption),
      shuttleCost: getShuttleBallCost(ballChangeOption),
      selected: true
    })
    setSelectedBall(ballChangeOption);
  };
  // Get shuttleCost
  const removePlayerFromCourtApi = (playerName, courtId, areaKey) => {
    const courtPayload = {
      courtId: courtId,
      courtAreas: [
        {
          area: areaKey,
          playerInArea: {
            playerName: playerName,
          },
        },
      ],
    };
    api.post(`/court-mana/removePlayerFromCourt`, courtPayload);
    // .then((res) => {
    //   if (responseSuccess(res)) {

    //   }
    // })
    // .catch((error) => {
    //   console.error(`Error while onDropPlayerBack - ${playerName}, ${courtId}, ${areaKey}, ${error} `);
    // });
    setCourts((prev) => {
      const updated = { ...prev };
      for (const id in updated) {
        for (const key in updated[id]) {
          if (updated[id][key] === playerName) updated[id][key] = null;
        }
      }
      return updated;
    });
    setAvailablePlayerBack(playerName);
  };

  const onDropPlayerOntoCourt = (
    playerName,
    courtId,
    areaKey,
    fromCourtId,
    fromArea
  ) => {
    const selectedBallValue = selectedBallRef.current;

    if (!selectedBallValue) {
      alert("Please select a shuttle ball before dropping a player.");
      return;
    }
    //
    // 1. If the player was already in another court → remove first
    if (fromCourtId != null && fromArea != null) {
      console.info(
        `Moving player from court ${fromCourtId}-${fromArea} to another court.`
      );
      removePlayerFromCourtApi(playerName, fromCourtId, fromArea);
    }

    // 2. api to add player to court area
    console.info(
      `Calling post api to add player:${playerName} to court:${courtId}-${areaKey}, selectedBallValue ${selectedBallValue}`
    );
    const ball = selectedBallValue
      .slice(0, selectedBallValue.lastIndexOf("-"))
      .trim();
    const cost = selectedBallValue
      .slice(selectedBallValue.lastIndexOf("-") + 1)
      .trim();
    const gameDTO = {
      playerName: playerName,
      court: {
        courtId: courtId,
        courtName: "",
        courtAreas: [
          {
            area: areaKey,
            playerInArea: {},
          },
        ],
      },
      shuttleBalls: [
        {
          shuttleName: ball,
          shuttleCost: parseFloat(cost),
        },
      ],
    };

    api.post(`/court-mana/addPlayerToCourt`, gameDTO);
    setCourts((prev) => {
      const updated = { ...prev };
      for (const id in updated) {
        for (const key in updated[id]) {
          if (updated[id][key] === playerName) updated[id][key] = null;
        }
      }
      updated[courtId][areaKey] = playerName;
      return updated;
    });
    setAvailablePlayers((prev) => prev.filter((p) => p !== playerName));
  };

  const setAvailablePlayerBack = (playerName) => {
    if (!availablePlayers.includes(playerName)) {
      // 2) add back to available list — ALWAYS return a NEW array reference
      setAvailablePlayers((prev) => {
        if (prev.includes(playerName)) {
          // force a new array so React re-renders and react-dnd unhides the item
          return [...prev];
        }
        return [...prev, playerName];
      });
    }
  };
  const setArrayAvailablePlayerBack = (arrayPlayer) => {
    Object.values(arrayPlayer).forEach((playerName) =>
      setAvailablePlayers((prev) => {
        if (prev.includes(playerName)) {
          // force a new array so React re-renders and react-dnd unhides the item
          return [...prev];
        }
        return [...prev, playerName];
      })
    );
  };
  const occupied = () => {};
  const onDropPlayerBack = (playerName, courtId, areaKey) => {
    console.log(availablePlayers);
    removePlayerFromCourtApi(playerName, courtId, areaKey);
  };

  const onAddPlayer = async (name) => {
    const addedPlayer = await api.post("/court-mana/addPlayer", name);
    if (addedPlayer.status === 200) {
      console.log("Adding new player successfully.");
      setAvailablePlayers((prev) => [...prev, name]);
      // set costInPerson
      handleDropService(name, "costInPerson", costInPerson);
    } else {
      console.error("Error while adding player to available session.");
      alert("Có lỗi khi thêm người chơi. Refresh lại trang này!");
    }
  };

  const handleDeletePlayer = async (selectedPlayer) => {
    console.log(availablePlayers);

    const deletedPlayer = await api.post(
      "/court-mana/removePlayer",
      selectedPlayer
    );
    if (deletedPlayer.status === 200) {
      console.log("Deleting player successfully.");
      setAvailablePlayers((prev) => prev.filter((p) => p !== selectedPlayer));
    } else {
      console.error("Error while deleting player to available session.");
      alert("Có lỗi khi xoa người chơi. Refresh lại trang này!");
    }
  };

  // Add shuttle ball area.
  const [showShuttleDialog, setShowShuttleDialog] = useState(false);
  const [courtProcessing, setCourtProcessing] = useState(0);
  const saveBallOntoCourt = (courtId, ballQuantityMap) => {
    // Build List<ShuttleBallDTO>
    const shuttleBallDTOList = Object.entries(ballQuantityMap).map(
      ([name, quantity]) => ({
        shuttleName: name.slice(0, name.lastIndexOf("-")).trim(),
        shuttleCost: Number(name.slice(name.lastIndexOf("-") + 1).trim()),
        ballQuantity: quantity,
      })
    );

    api
      .post(
        `/court-mana/addListBallIntoCourt?courtId=${courtId}`,
        shuttleBallDTOList
      )
      .then((res) => {
        if (responseDataTrue(res)) {
          setShowShuttleDialog(false);
        }
      })
      .catch((err) => console.error("Error fetching shuttle balls:", err));
    setShowShuttleDialog(false);
  };

  const handleCancel = () => {
    setShowShuttleDialog(false);
  };

  // Add ball into court by id.
  const showAddedBallDialog = (courtId) => {
    setCourtProcessing(courtId);
    setShowShuttleDialog(true);
  };

  /* On Start*/
  const [lockedCourts, setLockedCourts] = useState([]);
  const startGame = async (courtId) => {
    console.log(`Selected shuttle ball: ${selectedBall}`);

    await api
      .post(`/court-mana/changeGameState`, {
        court: {
          courtId: courtId,
        },
        shuttleBalls: [
          {
            shuttleName: getShuttleBallName(selectedBall),
            shuttleCost: getShuttleBallCost(selectedBall),
            quantity: 1,
          },
        ],
        gameState: "Start",
      })
      .then((response) => {
        if (response.status === 200) {
          console.log(
            `Response: ${response.data}. court [${courtId}] has been started.`
          );
          if (response.data === true) {
            setLockedCourts((prev) => ({ ...prev, [courtId]: true }));
          } else {
            alert(" Không thể bắt đầu. Số lượng người chơi mới không hợp lệ .");
          }
        }
      });
  };

  /** On Finish */
  const onFinish = async (courtId) => {
    const gameRes = await api.get(
      `/gameResult/getGameResult?courtId=${courtId}`
    );
    if (responseSuccess(gameRes)) {
      setGameDialogData(gameRes.data);
      setShowGameDialog(true);
    }
  };
  const confirmGameRes = async (formData) => {
    console.log("Confirmed action with data:", formData);
    // gameDialogData.state = "Finish";
    // const gameDTO = {
    //   playerName: "",
    //   court: {
    //     courtId: courtId,
    //     courtName: "San 2",
    //     courtAreas: [
    //       {
    //         area: "",
    //         playerInArea: {},
    //       },
    //     ],
    //   },
    //   shuttleBall: {
    //     shuttleName: "",
    //     shuttleCost: parseFloat(0),
    //   },
    // };
    // const res = await api.post(`/gameResult/confirmGameResult`, gameDialogData);

    // if (!responseSuccess(res)) {
    //   alert("Hành động thất bại. Load lại trang và thử lại. ");
    //   return;
    // }

    // setCourts((prev) => {
    //   const updated = { ...prev };
    //   const playersToReturn = Object.values(updated[courtId]).filter(Boolean);
    //   courtIds.forEach((id) => {
    //     for (const key in updated[id]) {
    //       if (playersToReturn.includes(updated[id][key]))
    //         updated[id][key] = null;
    //     }
    //   });
    //   return updated;
    // });

    // const courtId = gameDialogData.courtResult.courtId;
    // setAvailablePlayers((prev) => [
    //   ...prev,
    //   ...Object.values(courts[courtId]).filter(Boolean),
    // ]);
    // setLockedCourts((prev) => {
    //   const updated = { ...prev };
    //   delete updated[courtId];
    //   return updated;
    // });

    // alert(`On finish of court ${courtId}`);
    setShowGameDialog(false);
  };

  const cancelGameRes = () => {
    console.log("Cancel game");
    setShowGameDialog(false);
  };

  /**On Cancel */
  const onCancel = async (courtId) => {
    const res = await api.post(`/court-mana/changeGameState`, {
      court: {
        courtId: courtId,
      },
      gameState: "Cancel",
    });

    if (res.status !== 200 && res.data === false) {
      alert("Hành động thất bại. Load lại trang và thử lại. ");
      return;
    }

    setCourts((prev) => {
      const updated = { ...prev };
      const playersToReturn = Object.values(updated[courtId]).filter(Boolean);
      let playerName = null;
      courtIds.forEach((id) => {
        for (const key in updated[id]) {
          playerName = updated[id][key];
          if (playersToReturn.includes(playerName)) {
            setAvailablePlayerBack(playerName);
            updated[id][key] = null;
          }
        }
      });
      return updated;
    });

    // setAvailablePlayers((prev) => [
    //   ...prev,
    //   ...Object.values(courts[courtId]).filter(Boolean),
    // ]);
    setLockedCourts((prev) => ({
      ...prev,
      [courtId]: false,
    }));
    alert(`On Cancel court ${courtId}`);
  };

  // Handle Drop service
  const [playerServiceMap, setPlayerServiceMap] = useState({});
  const [selectedPlayer, setSelectedPlayer] = useState(null);
  const [showDialog, setShowDialog] = useState(false);

  const handleDropService = (playerName, serviceName, cost) => {
    if (!playerName) return;
    // add to db
    saveServiceToPlayer(playerName, serviceName, cost);

    // player: Array[Services]
    setPlayerServiceMap((prev) => {
      const existing = prev[playerName] || [];
      // if (existing.includes(serviceName)) return prev;
      return {
        ...prev,
        [playerName]: [...existing, serviceName.concat("-", cost)],
      };
    });
  };

  // Handle clicking on player
  const handleClickPlayer = (p) => {
    console.log(`Click on player:${p}`);
    setSelectedPlayer(p);
    setShowDialog(true);
  };
  const saveServiceToPlayer = async (playerName, serviceName, cost) => {
    const res = await api.post(
      `/court-mana/addServiceToPlayer?playerName=${playerName}`,
      {
        serviceName: serviceName,
        cost: parseFloat(cost),
      }
    );
    if (res.status === 200 && res.data === true) {
      console.info(`Added ${serviceName} - ${cost} to ${playerName}`);
    } else {
      console.error(`Failed to add ${serviceName} - ${cost} to ${playerName}`);
    }
  };

  // helper setter — ALWAYS use this to set selectedBall
  const setSelectedBallAndRef = (value) => {
    selectedBallRef.current = value; // update ref immediately
    setSelectedBall(value); // update state as well
  };

  // HomePage useEffect
  useEffect(() => {
    const fetchCourtInfor = async () => {
      try {
        // check available session
        const avaSession = await api.get("/session/checkAvailable");
        if (avaSession.status === 200) {
          if (avaSession.data === true) {
            console.info("Session is available.");
          } else {
            // create new one
            await api.post("/session/createNewSession");
            console.info("New session is created.");
            // await api.post('/session/deleteSession');
          }
        } else {
          // TODO
          console.error("Error while checking available session.");
          return;
        }

        // fetch shuttle_balls
        const ballResponse = await api.get("/court-mana/getShuttleBalls");
        if (ballResponse.status === 200 && ballResponse.data !== "") {
          const listBall = ballResponse.data;
          setBallOptions(
            listBall.map((b) => `${b.shuttleName} - ${b.shuttleCost}`)
          );
          // choose a sensible default (first item if exists)
          const selectedShuttleBall = listBall.find((b) => b.selected);
          let selectedShuttleBallOption = `${listBall[0].shuttleName} - ${listBall[0].shuttleCost}`;
          if (selectedShuttleBall) {
            selectedShuttleBallOption = `${selectedShuttleBall.shuttleName} - ${selectedShuttleBall.shuttleCost}`;
          }
          setSelectedBallAndRef(selectedShuttleBallOption);
        } else {
          console.error(ballResponse.message);
          setBallOptions([
            "Vinastar - 28000",
            "Cau 88 - 29000",
            "Sunlight Hall - 31000",
          ]);
        }

        // fetch services
        const servicesResponse = await api.get("/court-mana/getServices");
        console.log(
          `Getting service response status:${servicesResponse.status}`
        );
        let costPersonService = "";
        if (servicesResponse.status === 200 && servicesResponse.data !== "") {
          const listService = servicesResponse.data;
          // set cost in person and remainning services
          costPersonService = listService.find(
            (s) => s.serviceName === "costInPerson"
          ).cost;
          setCostInPerson(costPersonService);
          setServices(
            listService.filter((s) => s.serviceName !== "costInPerson")
          );
        }

        // fetch game DTO
        const resCourtMana = await api.get(`/court-mana/getCourtManagement`);
        if (resCourtMana.status === 200 && resCourtMana.data !== "") {
          const resGames = resCourtMana.data.gameDTOs;
          // const resCourts = resCourtMana.data.remainCourts;
          if (resGames !== "") {
            setCourts((prev) => {
              const currCourts = { ...prev };
              resGames.forEach((g) => {
                const id = parseInt(g.court.courtId);
                g.court.courtAreas.forEach((courtArea) => {
                  // set player onto area of court
                  currCourts[id][courtArea.area] =
                    courtArea.playerInArea.playerName;
                });
                // set lock court if gameState is Start
                if (g.gameState === "Start") {
                  setLockedCourts((prev) => ({
                    ...prev,
                    [g.court.courtId]: true,
                  }));
                }
              });
              return currCourts;
            });
          }
          console.info(`Courts:${courts}`);

          const resAvaPlayers = resCourtMana.data.availablePlayerDTOs;
          if (resAvaPlayers !== "") {
            setAvailablePlayers(resAvaPlayers.map((p) => p.playerName));

            resAvaPlayers.forEach((player) => {
              const playerServiceList = player.serviceDTOs.map((service) => {
                if (service.includes(COST_IN_PERSON)) {
                  return service.replace(COST_IN_PERSON, VN_COST_IN_PERSON);
                }
                return service;
              });

              setPlayerServiceMap((prevMap) => {
                return {
                  ...prevMap,
                  [player.playerName]: playerServiceList,
                };
              });
            });
          }
        } else {
          console.error(`Cannot get court management data.`);
        }
        // fetch available players in current session
        // const avaPlayers = await api.get("/court-mana/getAvailablePlayers");
        // if (avaPlayers.status === 200) {
        //   setAvailablePlayers(avaPlayers.data.map((p) => p.playerName));
        // }
      } catch (error) {
        console.error(error);
      }
    };

    fetchCourtInfor();
    // scrolling to the end
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, []);

  return (
    <DndProvider backend={HTML5Backend}>
      <div style={{ display: "flex", flexDirection: "row", height: "100vh" }}>
        <div
          style={{
            display: "flex",
            flexDirection: "column",
            width: "20%",
            padding: "3px",
            borderRight: "1px solid #ccc",
            transition: "background-color 2s ease",
          }}
        >
          <div
            className="service-area"
            style={{
              // backgroundColor: "rgb(239 242 244)",
              color: "white",
              border: "1px solid #cde",
              margin: "5px 5px 5px 5px",
              padding: "5px 10px 5px",
              backgroundImage: 'url("whatsapp-wallpaper-3.jpg")',
              backgroundSize: "cover",
              fontSize: "16px",
            }}
          >
            <h5> Tiền sân và Cầu: </h5>
            <b>Tiền sân: {costInPerson} vnd</b>
            <br></br>
            <b>Cầu: {selectedBall} vnd</b>
          </div>
          <div className="service-area">
            <h5 style={{ margin: "15px 0 0 5px" }}> Thay đổi cầu: </h5>
            <select
              id="ballOptionId"
              name="ballOptions"
              value={selectedBall}
              onChange={(e) => handleSelectedBall(e.target.value)}
              className="court-select selection-box"
            >
              {ballOptions.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
          </div>
          <div className="service-area">
            <h5 style={{ margin: "15px 0 0 5px" }}>Chọn dịch vụ: </h5>
            {services.map((s) => (
              <DraggableService
                key={s.serviceName}
                serviceName={s.serviceName}
                cost={s.cost}
              />
            ))}
          </div>
        </div>
        <div className="court-container" ref={scrollRef}>
          <div className="column court-bar">
            <PlayerArea
              availablePlayers={availablePlayers}
              onDropPlayerBack={onDropPlayerBack}
              onAddPlayer={onAddPlayer}
              newPlayer={newPlayer}
              setNewPlayer={setNewPlayer}
              onDropService={handleDropService}
              onClickPlayer={handleClickPlayer}
            />
          </div>
          {showDialog && (
            <ServiceDialog
              playerName={selectedPlayer}
              services={playerServiceMap[selectedPlayer] || []}
              onClose={() => setShowDialog(false)}
              onPay={() => alert("payment")}
              onDelete={handleDeletePlayer}
            />
          )}
          <div className="column left-column">
            {leftColumn
              .slice()
              .reverse()
              .map((id) => (
                <div key={id} className="image-card">
                  {/* <h1>Item{id}</h1> */}
                  <Court
                    key={id}
                    id={id}
                    players={courts[id]}
                    onDropPlayer={onDropPlayerOntoCourt}
                    occupied={occupied}
                    isLocked={lockedCourts[id]}
                    onStart={startGame}
                    showAddedBallDialog={showAddedBallDialog}
                    onFinish={onFinish}
                    onCancel={onCancel}
                    onDropService={handleDropService}
                  />
                </div>
              ))}
          </div>
          <div className="column right-column">
            {/* Render in reverse order to achieve "right-end > up" visual stacking */}
            {rightColumn
              .slice()
              .reverse()
              .map((id) => (
                <div key={id} className="image-card">
                  {/* <h1>Item{id}</h1> */}
                  <Court
                    key={id}
                    id={id}
                    players={courts[id]}
                    onDropPlayer={onDropPlayerOntoCourt}
                    occupied={occupied}
                    isLocked={lockedCourts[id]}
                    onStart={startGame}
                    showAddedBallDialog={showAddedBallDialog}
                    onFinish={onFinish}
                    onCancel={onCancel}
                    onDropService={handleDropService}
                  />
                </div>
              ))}
          </div>

          {/* Show add shuttle ball dialog */}
          <ShuttleBallDialog
            courtProcessing={courtProcessing}
            show={showShuttleDialog}
            onSaveBallOntoCourt={saveBallOntoCourt}
            onCancel={handleCancel}
          />

          {/* Show game dialog */}
          {showGameDialog && (
            <GameDialog
              show={showGameDialog}
              data={gameDialogData}
              onConfirm={confirmGameRes}
              onCancel={cancelGameRes}
            />
          )}
        </div>
      </div>
    </DndProvider>
  );
}

export default HomePage;
