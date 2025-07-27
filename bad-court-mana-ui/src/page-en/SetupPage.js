// src/LoginPage.js
import { useContext, useEffect, useState, useRef } from "react";

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
import { AuthContext } from "../context/AuthContext";
import axios from "axios";

function SetupPage() {
  const { csrfToken } = useContext(AuthContext);

  const currentHost = `${window.location.protocol}//${window.location.hostname}`;
  const localHost = "http://localhost:9080";
  const context = "bad-court-management-dev";

  const [totalCourt, setTotalCourt] = useState(7);
  const [costInPerson, setCostInPerson] = useState(12000);
  // const [shuttleCost, setShuttleCost] = useState("");
  const [shuttleBall, setShuttleBall] = useState([]);
  const [services, setServices] = useState([]);
  const [tableShuttleBalls, setTableShuttleBalls] = useState([]);
  const [tableServices, setTableServices] = useState([]);
  const tableRef = useRef(null);

  const handleAdd = () => {
    setServices([...services, { id: Date.now(), serviceName: "", cost: "" }]);
  };
  const handleRemove = (id) => {
    setServices(services.filter((service) => service.id !== id));
  };

  // for shuttle ball
  const [oneBall, setOneBall] = useState(true);
  const addShuttleBall = () => {
    if (oneBall === false) return;

    setShuttleBall([
      ...shuttleBall,
      { id: Date.now(), shuttleName: "", shuttleCost: "" },
    ]);
    setOneBall(false);
  };
  const removeShuttleBall = (id) => {
    setShuttleBall(shuttleBall.filter((ball) => ball.id !== id));
    setOneBall(true);
  };
  const handleBallChange = (id, field, value) => {
    setShuttleBall(
      shuttleBall.map((ball) =>
        ball.id === id ? { ...ball, [field]: value } : ball
      )
    );
  };
  const handleServiceChange = (id, field, value) => {
    setServices(
      services.map((service) =>
        service.id === id ? { ...service, [field]: value } : service
      )
    );
  };

  // service row clicking
  const [selectedRow, setSelectedRow] = useState(null);
  const [deletingRow, setDeletingRow] = useState(false);
  const handleRowClick = (index) => {
    setSelectedRow(index === selectedRow ? null : index);
  };
  // service delete handling
  const handleDelete = async (serviceToDelete, index) => {
    try {
      setDeletingRow(true);
      const res = await api.put("/api/deleteService", {
        serviceName: serviceToDelete.serviceName,
        cost: serviceToDelete.cost,
      });

      if (res.status === 200) {
        setTableServices((prev) => prev.filter((_, i) => i !== index));
        setSelectedRow(null);
      }
    } catch (err) {
      console.error("Error deleting service:", err);
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
  const handleDeleteBall = async (ballToDelete, index) => {
    try {
      setDeletingBall(true);
      const res = await api.put("/api/deleteShuttleBall", {
        shuttleName: ballToDelete.shuttleName,
        shuttleCost: ballToDelete.shuttleCost,
      });

      if (res.status === 200) {
        setTableShuttleBalls((prev) => prev.filter((_, i) => i !== index));
        setSelectedBall(null);
      }
    } catch (err) {
      console.error("Error deleting shuttle ball:", err);
    } finally {
      setDeletingBall(false);
    }
  };

  const handleSave = async () => {
    const payload = {
      totalCourt: totalCourt,
      costInPerson: costInPerson,
      shuttleBalls: shuttleBall.map(({ shuttleName, shuttleCost }) => ({
        shuttleName: shuttleName,
        shuttleCost: parseFloat(shuttleCost),
      })),
      services: services.map(({ serviceName, cost }) => ({
        serviceName: serviceName,
        cost: parseFloat(cost),
      })),
    };

    try {
      const response = await api.post(`/api/addSetupService`, payload, {});
      if (response.status === 200) {
        alert("Services saved successfully!");
        setTableServices((prevServices) => [...prevServices, ...services]);
        setTableShuttleBalls((prevShuttleBalls) => [
          ...prevShuttleBalls,
          ...shuttleBall,
        ]);
        // window.location.reload();
      }
    } catch (error) {
      alert("Failed to save services.");
    }
  };

  const fetchEntries = async () => {
    try {
      const res = await api.get(`/api/getSetupServices`);
      console.log(`return code:${res.status}`);
      console.log(`table:${tableServices.length}`);
      if (res.status === 200 && res.data !== "") {
        setTotalCourt(res.data.totalCourt);
        setCostInPerson(res.data.costInPerson);
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
          <h1>Setup page</h1>
        </Col>
        <Col sm={4}>
          <Button variant="primary" onClick={handleSave}>
            Save setting
          </Button>
        </Col>
      </Row>
      <Form>
        <Form.Group as={Row} className="md-3">
          <Form.Label column sm="1">
            {" "}
            Tong san:
          </Form.Label>
          <Col sm="4">
            <InputGroup className="mb-3">
              <Form.Control
                type="number"
                defaultValue="7"
                value={totalCourt}
                onChange={(e) => setTotalCourt(e.target.value)}
              />
              <InputGroup.Text>san</InputGroup.Text>
            </InputGroup>
          </Col>
        </Form.Group>
        <Form.Group as={Row} className="mb-3">
          <Form.Label column sm="1">
            Tien san:
          </Form.Label>
          <Col sm="4">
            <InputGroup className="mb-3">
              <Form.Control
                type="number"
                defaultValue="12000"
                value={costInPerson}
                onChange={(e) => setCostInPerson(e.target.value)}
              />
              <InputGroup.Text>vnd/nguoi</InputGroup.Text>
            </InputGroup>
          </Col>
        </Form.Group>

        {/* Add shuttleBall */}
        <Row className="row-space">
          <Col>
            <Button variant="success" onClick={addShuttleBall} className="me-2">
              + Add Shuttle ball
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
              Loai cau:
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
                  type="number"
                  defaultValue="26000"
                  value={ball.shuttleCost}
                  onChange={(e) =>
                    handleBallChange(ball.id, "shuttleCost", e.target.value)
                  }
                />
                <InputGroup.Text>vnd/trai</InputGroup.Text>
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
            <Row className="justify-content-start">
              <Col xs={6}>
                <h3>Loai cau</h3>
                <Table striped bordered hover size="sm" ref={tableRef}>
                  <thead>
                    <tr>
                      <th>Loai cau</th>
                      <th>Gia</th>
                      <th style={{ width: "50px" }}></th>
                    </tr>
                  </thead>
                  <tbody>
                    {tableShuttleBalls.map((ball, idx) => (
                      <tr
                        key={idx}
                        onClick={() => handleBallClick(idx)}
                        className={selectedBall === idx ? "table-active" : ""}
                        style={{ cursor: "pointer" }}
                      >
                        <td>{ball.shuttleName}</td>
                        <td>{ball.shuttleCost} vnd/trai</td>
                        <td>
                          {selectedBall === idx && (
                            <button
                              className="btn btn-outline-danger btn-sm"
                              onClick={(e) => {
                                e.stopPropagation(); // Prevent row re-select
                                handleDeleteBall(ball, idx);
                              }}
                              disabled={deletingBall}
                            >
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
              + Add Service
            </Button>
          </Col>
        </Row>
        {services.map((service, index) => (
          <Row key={service.id} className="align-items-center mb-2 row-space">
            <Form.Label column sm="1">
              Dich vu:
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
                  type="number"
                  value={service.cost}
                  onChange={(e) =>
                    handleServiceChange(service.id, "cost", e.target.value)
                  }
                />
                <InputGroup.Text>vnd</InputGroup.Text>
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
                <FormLabel>Dich vu</FormLabel>
                <Table striped bordered hover size="sm" ref={tableRef}>
                  <thead>
                    <tr>
                      <th>Service name</th>
                      <th>Service cost</th>
                      <th style={{ width: "50px" }}></th>
                    </tr>
                  </thead>
                  <tbody>
                    {tableServices.map((ser, index) => (
                      <tr
                        key={index}
                        onClick={() => handleRowClick(index)}
                        className={selectedRow === index ? "table-active" : ""}
                        style={{ cursor: "pointer" }}
                      >
                        <td>{ser.serviceName}</td>
                        <td>{ser.cost}</td>
                        <td>
                          {selectedRow === index && (
                            <button
                              className="btn btn-outline-danger btn-sm"
                              onClick={(e) => {
                                e.stopPropagation(); // Prevent row re-select
                                handleDelete(ser, index);
                              }}
                              disabled={deletingRow}
                            >
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
              Save setting
            </Button>
          </Col>
        </Row>
      </Form>
    </Container>
  );
}

export default SetupPage;
