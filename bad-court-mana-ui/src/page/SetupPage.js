// src/LoginPage.js
import { useEffect, useState, useRef } from "react";

import {
  Form,
  Row,
  Col,
  Button,
  Container,
  InputGroup,
  FormLabel,
  Table,
} from "react-bootstrap";
import api from "../api/index";

import {VN_CURRENCY, formatVND, rawNumber } from "./MoneyUtils";

function SetupPage() {
  const [errorMess, setErrorMess] = useState(null);
  const [totalCourt, setTotalCourt] = useState(7);
  const [costInPerson, setCostInPerson] = useState();
  const [costInPersonFormatted, setcostInPersonFormatted] = useState("");
  const [costInPersonEditing, setCostInPersonEditing] = useState(false);
  const [shuttleBall, setShuttleBall] = useState([]);
  const [services, setServices] = useState([]);
  const [tableShuttleBalls, setTableShuttleBalls] = useState([]);
  const [tableServices, setTableServices] = useState([]);
  const tableRef = useRef(null);
  const tableBallRef = useRef(null);


  const handleAdd = () => {
    setServices([...services, { id: Date.now(), serviceName: "", cost: "" }]);
  };
  const handleRemove = (id) => {
    setServices(services.filter((service) => service.id !== id));
  };

  // for shuttle ball
  const [oneBall, setOneBall] = useState(true);
  const addShuttleBall = () => {
    if (oneBall === false) {
      setErrorMess("Chỉ được thêm 1 loại cầu trong 1 lần.");
      return;
    }
    setShuttleBall([
      ...shuttleBall,
      { id: Date.now(), shuttleName: "", cost: "", costFormat: "" },
    ]);
    setOneBall(false);
  };
  const removeShuttleBall = (id) => {
    setShuttleBall(shuttleBall.filter((ball) => ball.id !== id));
    setOneBall(true);
  };

  const handleBallChange = (id, field, value) => {
    setShuttleBall(
      shuttleBall.map((ball) => {
        if (ball.id === id) {
          if (field === "cost") {
            const raw = Number(value.toString().replace(/\D/g, ""));
            return {
              ...ball,
              cost: raw,
              costFormat: formatVND(raw),
            };
          }
          return { ...ball, [field]: value };
        }
        return ball;
      })
    );
  };
  const handleServiceChange = (id, field, value) => {
    setServices(
      services.map((service) => {
        if (service.id === id) {
          if (field === "cost") {
            const raw = Number(value.toString().replace(/\D/g, ""));
            return {
              ...service,
              cost: raw,
              costFormat: formatVND(raw),
            };
          }
          return { ...service, [field]: value };
        }
        return service;
      })
    );
  };
  const handleServiceCostChange = (e, id) => {
    const raw = e.target.value.replace(/\D/g, "");
    handleServiceChange(id, "cost", raw);
  };
  // service row clicking
  const [selectedRow, setSelectedRow] = useState(null);
  const [deletingRow, setDeletingRow] = useState(false);
  const handleRowClick = (index) => {
    setSelectedRow(index === selectedRow ? null : index);
  };
  // service delete handling
  const handleDeleteService = (serviceToDelete, idx) => {
    try {
      setDeletingRow(true);
      // const res = await api.put("/api/deleteService", {
      //   serviceName: serviceToDelete.serviceName,
      //   cost: serviceToDelete.cost,
      // });

      // if (res.status === 200) {
      setTableServices((prev) =>
        prev.map((item, i) =>
          i === idx ? { ...item, isDeleted: !item.isDeleted } : item
        )
      );
      setSelectedRow(null);
      // }
    } catch (err) {
      console.error("Error deleting service:", err);
      setErrorMess(`Có lỗi khi xoá dich vụ:${serviceToDelete.serviceName}`);
    } finally {
      setDeletingRow(false);
    }
  };
  // shuttle ball row clicking
  const [selectedBall, setSelectedBall] = useState(null);
  const [deletingBall, setDeletingBall] = useState(false);
  const handleBallClick = (index) => {
    setSelectedBall(index === selectedBall ? null : index);
  };
  // shuttle ball delete handling
  const handleDeleteBall = (ball, idx) => {
    try {
      setDeletingBall(true);
      // const res = await api.put("/api/deleteShuttleBall", {
      //   shuttleName: ball.shuttleName,
      //   shuttleCost: ball.shuttleCost,
      // });

      // if (res.status === 200) {+++++++++++++++
      // setTableShuttleBalls((prev) => prev.filter((_, i) => i !== idx));
      setTableShuttleBalls((prev) =>
        prev.map((item, i) =>
          i === idx ? { ...item, isDeleted: !item.isDeleted } : item
        )
      );
      setSelectedBall(null);
    } catch (err) {
      console.error("Error deleting shuttle ball:", err);
      setErrorMess(`Có lỗi khi xoá loại cầu: ${ball.shuttleName}`);
    } finally {
      setDeletingBall(false);
    }
  };

  const existedInTableService = (newService) => {
    const found = tableServices.some((s) => {
      return (
        s.serviceName === newService.serviceName &&
        s.cost == parseFloat(newService.cost)
      );
    });
    return found;
  };
  const existedInTableShuttleBall = (newBall) => {
    const found = tableShuttleBalls.some((b) => {
      return (
        b.shuttleName === newBall.shuttleName &&
        b.shuttleCost == parseFloat(newBall.shuttleCost)
      );
    });
    return found;
  };

  // const [isDuplicated, setIsDuplicated] = useState(false);
  const handleSave = async () => {
    // setIsDuplicated(false);
    setErrorMess(null);

    const duplicates = [];
    const duplicatesBall = [];
    services.forEach((newService) => {
      if (existedInTableService(newService)) {
        duplicates.push(newService.serviceName);
      }
    });
    shuttleBall.forEach((newBall) => {
      if (existedInTableShuttleBall(newBall)) {
        duplicatesBall.push(newBall.shuttleName);
      }
    });
    let isDuplicated = false;
    let mess = "";
    if (duplicates.length > 0) {
      mess = `Tên của Dịch vụ đang trùng lặp: ${duplicates.join(", ")}`;
      isDuplicated = true;
    }
    if (duplicatesBall.length > 0) {
      mess +=
        "\n." + `Tên quả cầu lông đang trùng lặp: ${duplicatesBall.join(", ")}`;
      isDuplicated = true;
    }
    if (isDuplicated) {
      setErrorMess(mess);
      alert("Không thể lưu thiết lập. Kiểm tra lỗi trùng lặp.");
      return;
    }
    const rawNumber =  Number(costInPerson.toString().replace(/\D/g, ""));
    const payload = {
      totalCourt: totalCourt,
      costInPerson: rawNumber,
      addedShuttleBalls: shuttleBall.map((b) => {
        return {
          shuttleName: b.shuttleName,
          shuttleCost: b.cost,
        };
      }),
      deletedShuttleBalls: tableShuttleBalls
        .filter((b) => b.isDeleted)
        .map(({ shuttleName, cost }) => ({
          shuttleName: shuttleName,
          shuttleCost: cost,
        })),
      addedServices: services,
      deletedServices: tableServices
        .filter((s) => s.isDeleted)
        .map(({ serviceName, cost }) => ({
          serviceName: serviceName,
          cost: parseFloat(cost),
        })),
    };

    try {
      const response = await api.post(`/api/updateSetupService`, payload, {});
      if (response.status === 200) {
        alert("Lưu thiết lập thành công!");
        setTableShuttleBalls([
          ...tableShuttleBalls.filter((ball) => !ball.isDeleted),
          ...shuttleBall,
        ]);

        setTableServices([
          ...tableServices.filter((ser) => !ser.isDeleted),
          ...services,
        ]);

        // remove input fields of shuttle_ball and service
        setShuttleBall([]);
        setOneBall(true);
        setServices([]);
        setCostInPersonEditing(false);
      } else {
        setErrorMess(`${response.data.message}`);
      }
    } catch (error) {
      alert("Có lỗi khi lưu thiết lập. Kiểm tra lỗi màu đỏ bên dưới.");
    }
  };

  const handleCostInPerson = (e) => {
    const raw = e.target.value.replace(/\D/g, "");
    setCostInPerson(raw);
    setcostInPersonFormatted(formatVND(raw));
  };
  const handleCostInput = (e, id) => {
    const raw = e.target.value.replace(/\D/g, ""); // remove non digits
    handleBallChange(id, "cost", raw);
  };
  const fetchEntries = async () => {
    try {
      const res = await api.get(`/api/getSetupServices`);
      console.log(`return code:${res.status}`);
      console.log(`table:${tableServices.length}`);
      if (res.status === 200 && res.data !== "") {
        setTotalCourt(res.data.totalCourt);
        setCostInPerson(res.data.costInPerson);
        setcostInPersonFormatted(formatVND(res.data.costInPerson));
        setTableShuttleBalls(res.data.shuttleBalls);
        setTableServices(res.data.services);
      }
    } catch (error) {
      console.error(error);
    }
  };

  useEffect(() => {
    fetchEntries();

    const handleClickOutside = (event) => {
      if (tableRef.current && !tableRef.current.contains(event.target)) {
        setSelectedRow(null); // clear selection
      }

      if (
        tableBallRef.current &&
        !tableBallRef.current.contains(event.target)
      ) {
        setSelectedBall(null);
      }
    };

    document.addEventListener("click", handleClickOutside, true);

    return () => {
      document.removeEventListener("click", handleClickOutside, true);
    };
  }, []);

  return (
    <Container>
      <Row>
        <Col sm={6}>
          <h1>Trang thiết lâp sân cầu</h1>
        </Col>
        <Col sm={4}>
          <Button variant="primary" onClick={handleSave}>
            Lưu thiết lập
          </Button>
        </Col>
      </Row>
      <Form>
        <Form.Group as={Row} className="md-3">
          <Form.Label column sm="1">
            {" "}
            Tổng sân:
          </Form.Label>
          <Col sm="4">
            <InputGroup className="mb-3">
              <Form.Control
                type="number"
                defaultValue="8"
                // value={totalCourt}
                onChange={(e) => setTotalCourt(e.target.value)}
                disabled={true}
              />
              <InputGroup.Text>sân</InputGroup.Text>
            </InputGroup>
          </Col>
        </Form.Group>
        <Form.Group as={Row} className="mb-3">
          <Form.Label column sm="1">
            Tiền sân:
          </Form.Label>
          <Col sm="4">
            <InputGroup className="mb-3">
              {costInPersonEditing ? (
                <Form.Control
                  type="text"
                  value={costInPerson}
                  onChange={(e) => handleCostInPerson(e)}
                  autoFocus
                />
              ) : (
                <Form.Control
                  type="text"
                  value={costInPersonFormatted}
                  readOnly
                  onClick={() => setCostInPersonEditing(true)}
                  style={{ cursor: "pointer" }}
                />
              )}
              <InputGroup.Text>{VN_CURRENCY}/người</InputGroup.Text>
            </InputGroup>
          </Col>
        </Form.Group>
        {/* Error message */}
        <Row>
          <Col>{errorMess && <p id="danger">{errorMess}</p>}</Col>
        </Row>
        {/* Add shuttleBall */}
        <Row className="row-space">
          <Col>
            <Button variant="success" onClick={addShuttleBall} className="me-2">
              + Thêm loại cầu
            </Button>
          </Col>
        </Row>

        {shuttleBall.map((ball, index) => (
          <Form.Group
            as={Row}
            key={ball.id}
            className="align-items-center mb-2 row-space"
          >
            <Form.Label column sm="1">
              Loại cầu:
            </Form.Label>
            <Col sm={3}>
              <Form.Control
                type="text"
                placeholder="VinaStar"
                value={ball.shuttleName}
                onChange={(e) =>
                  handleBallChange(ball.id, "shuttleName", e.target.value)
                }
              />
            </Col>
            <Col sm={3}>
              <InputGroup className="mb-2">
                <Form.Control
                  type="text"
                  value={formatVND(ball.cost)}
                  onChange={(e) => handleCostInput(e, ball.id)}
                />
                <InputGroup.Text>{VN_CURRENCY}/trái</InputGroup.Text>
              </InputGroup>
            </Col>
            <Col md={1}>
              <Button
                variant="danger"
                onClick={() => removeShuttleBall(ball.id)}
              >
                -
              </Button>
            </Col>
          </Form.Group>
        ))}
        {/* Table existed shuttle balss */}
        {tableShuttleBalls.length !== 0 && (
          <>
            <Row className="justify-content-start" id="cus-table">
              <Col xs={6}>
                <h3>Danh sách Loại cầu</h3>
                <Table striped bordered hover size="sm" ref={tableBallRef}>
                  <thead>
                    <tr>
                      <th>Loại cầu</th>
                      <th>Giá ({VN_CURRENCY}/trái)</th>
                      <th style={{ width: "50px" }}>Xoá</th>
                    </tr>
                  </thead>
                  <tbody>
                    {tableShuttleBalls.map((ball, idx) => (
                      <tr
                        key={idx}
                        onClick={() => !ball.isDeleted && handleBallClick(idx)}
                        className={selectedBall === idx ? "table-active" : ""}
                        style={{
                          cursor: ball.isDeleted ? "not-allowed" : "pointer",
                          textDecoration: ball.isDeleted
                            ? "line-through"
                            : "none",
                          opacity: ball.isDeleted ? 0.5 : 1,
                          transition: "all 0.2s ease", // Smooth transition for the strike-through
                        }}
                      >
                        <td>{ball.shuttleName}</td>
                        <td>{ball.costFormat}</td>
                        <td>
                          {/* display the delete/undo button */}
                          {(selectedBall === idx || ball.isDeleted) && (
                            <button
                              title={`${ball.isDeleted ? "Quay lại" : "Xoá"}`}
                              className={`btn btn-sm ${
                                ball.isDeleted
                                  ? "btn-outline-primary"
                                  : "btn-outline-danger"
                              }`}
                              onClick={(e) => {
                                e.stopPropagation();
                                handleDeleteBall(ball, idx);
                              }}
                              disabled={deletingBall}
                            >
                              {ball.isDeleted ? (
                                <svg
                                  xmlns="http://www.w3.org/2000/svg"
                                  width="16"
                                  height="16"
                                  fill="currentColor"
                                  class="bi bi-arrow-counterclockwise"
                                  viewBox="0 0 16 16"
                                >
                                  <path
                                    fill-rule="evenodd"
                                    d="M8 3a5 5 0 1 1-4.546 2.914.5.5 0 0 0-.908-.417A6 6 0 1 0 8 2z"
                                  />
                                  <path d="M8 4.466V.534a.25.25 0 0 0-.41-.192L5.23 2.308a.25.25 0 0 0 0 .384l2.36 1.966A.25.25 0 0 0 8 4.466" />
                                </svg>
                              ) : (
                                <svg
                                  xmlns="http://www.w3.org/2000/svg"
                                  width="16"
                                  height="16"
                                  fill="currentColor"
                                  className="bi bi-trash"
                                  viewBox="0 0 16 16"
                                >
                                  <path d="M5.5 5.5A.5.5 0 0 1 6 6v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5m2.5 0a.5.5 0 0 1 .5.5v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5m3 .5a.5.5 0 0 0-1 0v6a.5.5 0 0 0 1 0z" />
                                  <path d="M14.5 3a1 1 0 0 1-1 1H13v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V4h-.5a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1H6a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1h3.5a1 1 0 0 1 1 1zM4.118 4 4 4.059V13a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1V4.059L11.882 4zM2.5 3h11V2h-11z" />
                                </svg>
                              )}
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </Table>
              </Col>
            </Row>
          </>
        )}

        {/* Add service area */}
        <Row className="row-space">
          <Col>
            <Button variant="success" onClick={handleAdd} className="me-2">
              + Thêm dich vụ
            </Button>
          </Col>
        </Row>
        {services.map((service) => (
          <Row key={service.id} className="align-items-center mb-2 row-space">
            <Form.Label column sm="1">
              Dịch vụ:
            </Form.Label>
            <Col sm={3}>
              <Form.Control
                type="text"
                value={service.serviceName}
                onChange={(e) =>
                  handleServiceChange(service.id, "serviceName", e.target.value)
                }
              />
            </Col>

            <Col sm={3}>
              <InputGroup className="mb-2">
                <Form.Control
                  type="text"
                  value={formatVND(service.cost)}
                  onChange={(e) => handleServiceCostChange(e, service.id)}
                />
                <InputGroup.Text>{VN_CURRENCY}</InputGroup.Text>
              </InputGroup>
            </Col>

            <Col sm={1}>
              <Button variant="danger" onClick={() => handleRemove(service.id)}>
                -
              </Button>
            </Col>
          </Row>
        ))}

        {/* Table existed services */}
        {tableServices.length !== 0 && (
          <>
            <Row className="justify-content-start" id="cus-table">
              <Col xs={6}>
                <h3>Danh sách Dịch vụ</h3>
                <Table striped bordered hover size="sm" ref={tableRef}>
                  <thead>
                    <tr>
                      <th>Dịch vụ</th>
                      <th>Giá ({VN_CURRENCY})</th>
                      <th style={{ width: "50px" }}>Xoá </th>
                    </tr>
                  </thead>
                  <tbody>
                    {tableServices.map((ser, index) => (
                      <tr
                        key={index}
                        onClick={() => !ser.isDeleted && handleRowClick(index)}
                        className={selectedRow === index ? "table-active" : ""}
                        style={{
                          cursor: ser.isDeleted ? "not-allowed" : "pointer",
                          textDecoration: ser.isDeleted
                            ? "line-through"
                            : "none",
                          opacity: ser.isDeleted ? 0.5 : 1,
                          transition: "all 0.2s ease", // Smooth transition for the strike-through
                        }}
                      >
                        <td>{ser.serviceName}</td>
                        <td>{ser.costFormat}</td>
                        <td>
                          {(selectedRow === index || ser.isDeleted) && (
                            <button
                              title={`${ser.isDeleted ? "Quay lại" : "Xoá"}`}
                              className={`btn btn-sm ${
                                ser.isDeleted
                                  ? "btn-outline-primary"
                                  : "btn-outline-danger"
                              }`}
                              onClick={(e) => {
                                e.stopPropagation(); // Prevent row re-select
                                handleDeleteService(ser, index);
                              }}
                              disabled={deletingRow}
                            >
                              {ser.isDeleted ? (
                                <svg
                                  xmlns="http://www.w3.org/2000/svg"
                                  width="16"
                                  height="16"
                                  fill="currentColor"
                                  class="bi bi-arrow-counterclockwise"
                                  viewBox="0 0 16 16"
                                >
                                  <path
                                    fill-rule="evenodd"
                                    d="M8 3a5 5 0 1 1-4.546 2.914.5.5 0 0 0-.908-.417A6 6 0 1 0 8 2z"
                                  />
                                  <path d="M8 4.466V.534a.25.25 0 0 0-.41-.192L5.23 2.308a.25.25 0 0 0 0 .384l2.36 1.966A.25.25 0 0 0 8 4.466" />
                                </svg>
                              ) : (
                                <svg
                                  xmlns="http://www.w3.org/2000/svg"
                                  width="16"
                                  height="16"
                                  fill="currentColor"
                                  class="bi bi-trash"
                                  viewBox="0 0 16 16"
                                >
                                  <path d="M5.5 5.5A.5.5 0 0 1 6 6v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5m2.5 0a.5.5 0 0 1 .5.5v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5m3 .5a.5.5 0 0 0-1 0v6a.5.5 0 0 0 1 0z" />
                                  <path d="M14.5 3a1 1 0 0 1-1 1H13v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V4h-.5a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1H6a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1h3.5a1 1 0 0 1 1 1zM4.118 4 4 4.059V13a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1V4.059L11.882 4zM2.5 3h11V2h-11z" />
                                </svg>
                              )}
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </Table>
              </Col>
            </Row>
          </>
        )}
        <Row className="row-space">
          <Col sm={4}>
            <Button variant="primary" onClick={handleSave}>
              Lưu thiết lập
            </Button>
          </Col>
        </Row>
      </Form>
    </Container>
  );
}

export default SetupPage;
